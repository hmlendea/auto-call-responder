package ro.nucilandia.autocallresponder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class CallHandlingService : Service() {

    private val TAG = "CallHandlingService"
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var callJob: Job? = null

    /** State of the call currently being handled, so the notification is sent exactly once. */
    private class CallSession(val callerNumber: String) {
        val ringTimeMillis: Long = System.currentTimeMillis()
        var answerTimeMillis: Long = 0L
        var isNotified: Boolean = false
    }

    @Volatile
    private var activeSession: CallSession? = null

    companion object {
        const val CHANNEL_ID = "CallHandlingChannel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_CALLER_NUMBER = "caller_number"
        const val EXTRA_DELAY_SECONDS = "delay_seconds"

        const val MILLIS_PER_SECOND = 1000L
        const val ANSWER_SETTLE_MILLISECONDS = 1_500L
        const val AUDIO_END_DELAY_MILLISECONDS = 1_000L
        const val AUDIO_PLAYBACK_TIMEOUT_SECONDS = 120L
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val callerNumber = intent?.getStringExtra(EXTRA_CALLER_NUMBER) ?: ""
        val delaySeconds = intent?.getIntExtra(EXTRA_DELAY_SECONDS, 0) ?: 0

        when (action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification(callerNumber))
                handleCall(callerNumber, delaySeconds)
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping the call handling because the call ended")
                endSession(callerNumber)
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Call Handling",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Handling incoming calls automatically"
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(callerNumber: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Auto Call Responder")
            .setContentText("Handling call from $callerNumber")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()
    }

    private fun handleCall(callerNumber: String, delaySeconds: Int) {
        val session = CallSession(callerNumber)
        activeSession = session

        callJob = scope.launch {
            val controller = CallAudioController(this@CallHandlingService)

            try {
                if (delaySeconds > 0) {
                    Log.d(TAG, "Waiting $delaySeconds seconds before answering $callerNumber")
                    delay(delaySeconds * MILLIS_PER_SECOND)
                }

                // Answer the call, then silence the microphone for the whole call.
                answerCall()
                session.answerTimeMillis = System.currentTimeMillis()
                controller.muteMicrophone()

                // Respect the configured post-answer delay before playback
                val internalDelay = SettingsManager.getSettings().internalDelaySeconds
                delay(ANSWER_SETTLE_MILLISECONDS + internalDelay * MILLIS_PER_SECOND)

                // Play audio and wait for it to actually finish
                playAudioFileAndAwait(controller)

                // End the call one second after the audio finished
                delay(AUDIO_END_DELAY_MILLISECONDS)
                endCall()
            } catch (cancellation: CancellationException) {
                Log.d(TAG, "Call handling cancelled because the call ended")
                throw cancellation
            } catch (e: Exception) {
                Log.e(TAG, "Error handling call", e)
            } finally {
                controller.restoreAfterPlayback()
                notifyCallHandled(session)
                stopSelf()
            }
        }
    }

    private fun answerCall() {
        try {
            val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "ANSWER_PHONE_CALLS permission not granted; cannot answer the call")
                return
            }
            @Suppress("DEPRECATION")
            telecomManager.acceptRingingCall()
            Log.d(TAG, "Answered the ringing call")
        } catch (e: Exception) {
            Log.e(TAG, "Error answering call", e)
        }
    }

    private suspend fun playAudioFileAndAwait(controller: CallAudioController) {
        val settings = SettingsManager.getSettings()
        val audioUriString = settings.audioFilePath
            .takeIf { it.isNotBlank() }
            ?: SettingsManager.getAudioFileUri()
            ?: return

        controller.prepareForPlayback(settings.earpieceOnly)

        val finished = CompletableDeferred<Unit>()

        try {
            val audioUri = Uri.parse(audioUriString)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(this@CallHandlingService, audioUri)
                setOnCompletionListener {
                    Log.d(TAG, "Audio playback completed")
                    finished.complete(Unit)
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Audio playback failed: what=$what, extra=$extra")
                    finished.complete(Unit)
                    true
                }
                prepare()
                // The microphone is unmuted only for the duration of the recording.
                controller.unmuteMicrophone()
                start()
            }

            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)

            // Suspends until the file finishes, so ending the call can cancel the wait; the timeout
            // is only a safety net.
            val completed = withTimeoutOrNull(AUDIO_PLAYBACK_TIMEOUT_SECONDS * MILLIS_PER_SECOND) {
                finished.await()
            }
            if (completed == null) {
                Log.w(TAG, "Audio playback timed out after $AUDIO_PLAYBACK_TIMEOUT_SECONDS seconds")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio", e)
        } finally {
            controller.muteMicrophone()
            stopAudio()
        }
    }

    @Synchronized
    private fun stopAudio() {
        mediaPlayer?.let { player ->
            runCatching { player.stop() }
            player.release()
        }
        mediaPlayer = null
    }

    /** Sends the notification for a call that ended before the responder could handle it. */
    private fun reportEndedCall(callerNumber: String) {
        notifyCallHandled(CallSession(callerNumber))
        stopSelf()
    }

    /** Stops playback, cancels every pending delay and still sends the notification. */
    private fun endSession(callerNumber: String) {
        val session = activeSession

        if (session == null) {
            // The service was started only to report a call that ended before the delay elapsed,
            // so the foreground notification has to be shown before the service stops itself.
            startForeground(NOTIFICATION_ID, createNotification(callerNumber))
            if (callerNumber.isNotEmpty()) {
                reportEndedCall(callerNumber)
            } else {
                stopSelf()
            }
            return
        }

        callJob?.cancel()
        callJob = null
        stopAudio()

        activeSession = null
        notifyCallHandled(session)

        stopSelf()
    }

    /** Sends the notification for the handled call exactly once. */
    @Synchronized
    private fun notifyCallHandled(session: CallSession) {
        if (session.isNotified) return
        session.isNotified = true

        // Sent only after the call is disconnected so SMTP never delays the ending
        EmailNotifier.sendCallNotification(
            callerNumber = session.callerNumber,
            ringTimeMillis = session.ringTimeMillis,
            answerTimeMillis = session.answerTimeMillis,
            endTimeMillis = System.currentTimeMillis()
        )
    }

    private fun endCall() {
        try {
            val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                Log.w(TAG, "Ending calls programmatically requires Android 9 or later")
                return
            }
            if (checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "ANSWER_PHONE_CALLS permission not granted; cannot end the call")
                return
            }
            telecomManager.endCall()
        } catch (e: Exception) {
            Log.e(TAG, "Error ending call", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudio()
        callJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
