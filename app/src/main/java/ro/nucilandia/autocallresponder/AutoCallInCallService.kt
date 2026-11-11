package ro.nucilandia.autocallresponder

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.util.Log

/** Handles incoming calls while the application is the default dialer. */
class AutoCallInCallService : InCallService() {

    private val tag = "AutoCallInCallService"
    private val handler = Handler(Looper.getMainLooper())
    private val sessions = mutableMapOf<String, CallSession>()
    private var mediaPlayer: MediaPlayer? = null
    private var audioController: CallAudioController? = null

    /**
     * State of a single handled call.
     *
     * Every delay is scheduled through [schedule], so the whole sequence can be cancelled as soon
     * as the call ends, and every scheduled action checks [isEnding] before it runs.
     */
    private inner class CallSession(
        val call: Call,
        val callerNumber: String
    ) {
        val identifier: String = callIdentifier(call)
        val ringTimeMillis: Long = System.currentTimeMillis()
        var answeredAtMillis: Long = 0L
        var isEnding: Boolean = false
        var isFinished: Boolean = false
        var isHandled: Boolean = false

        private val scheduledTasks = mutableListOf<Runnable>()

        fun schedule(delayMillis: Long, action: () -> Unit) {
            if (isEnding) return

            val runnable = Runnable {
                if (isEnding) return@Runnable
                action()
            }
            scheduledTasks.add(runnable)
            handler.postDelayed(runnable, delayMillis)
        }

        fun cancelScheduledTasks() {
            scheduledTasks.forEach { task -> handler.removeCallbacks(task) }
            scheduledTasks.clear()
        }
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            Log.d(tag, "Call state changed: $state")
            val session = sessions[callIdentifier(call)] ?: return

            when (state) {
                Call.STATE_RINGING -> scheduleAnswer(call)
                // Telecom resets the mute flag while the call transitions to active, so the mute
                // has to be re-applied after the answer transition settles.
                Call.STATE_ACTIVE -> audioController?.muteMicrophone()
                Call.STATE_DISCONNECTED -> finishSession(session)
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        call.registerCallback(callCallback)
        Log.d(tag, "Call added, handle=${call.details.handle}")
        if (call.state == Call.STATE_RINGING) scheduleAnswer(call)
    }

    override fun onCallRemoved(call: Call) {
        call.unregisterCallback(callCallback)
        sessions[callIdentifier(call)]?.let { session -> finishSession(session) }
        super.onCallRemoved(call)
    }

    private fun scheduleAnswer(call: Call) {
        val settings = SettingsManager.getSettings()
        if (!settings.enabled) return

        val callerNumber = callerNumber(call)
        if (settings.filterRomanianNumbers && !RomanianPhoneNumbers.isRomanian(callerNumber)) return

        val identifier = callIdentifier(call)
        if (sessions.containsKey(identifier)) return

        val session = CallSession(call, callerNumber)
        sessions[identifier] = session

        Log.d(tag, "Answering $callerNumber in ${settings.delaySeconds} seconds")
        session.schedule(settings.delaySeconds * MILLIS_PER_SECOND) { answer(session) }
    }

    private fun answer(session: CallSession) {
        val call = session.call
        val controller = CallAudioController(this, this)
        audioController = controller

        // Mutating the mute state before answering is undone by Telecom when the call becomes
        // active; muting again on STATE_ACTIVE makes it stick.
        controller.muteMicrophone()

        try {
            call.answer(VideoProfile.STATE_AUDIO_ONLY)
            session.answeredAtMillis = System.currentTimeMillis()
            Log.d(tag, "Answered call from ${session.callerNumber}")
        } catch (exception: Exception) {
            Log.e(tag, "Could not answer the call", exception)
            controller.unmuteMicrophone()
            audioController = null
            return
        }

        // Safety net in case the ACTIVE callback arrives before the controller is stored.
        session.schedule(MICROPHONE_MUTE_REAPPLY_MILLISECONDS) { controller.muteMicrophone() }

        val settings = SettingsManager.getSettings()
        val internalDelay = settings.internalDelaySeconds
        Log.d(tag, "Waiting $internalDelay seconds after answering before playback")

        session.schedule(ANSWER_SETTLE_MILLISECONDS + internalDelay * MILLIS_PER_SECOND) {
            controller.prepareForPlayback(earpieceOnly = settings.earpieceOnly)
            playAudio(session)
        }
    }

    private fun playAudio(session: CallSession) {
        val audioUriString = SettingsManager.getSettings().audioFilePath
            .takeIf { it.isNotBlank() }
            ?: SettingsManager.getAudioFileUri()

        if (audioUriString == null) {
            Log.w(tag, "No audio file configured, ending the call")
            endCall(session)
            return
        }

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(this@AutoCallInCallService, Uri.parse(audioUriString))
                setOnCompletionListener {
                    // The end-of-playback delay belongs to the session, so it is skipped when the
                    // call has already ended.
                    Log.d(tag, "Audio playback completed; ending the call in ${AUDIO_END_DELAY_MILLISECONDS / MILLIS_PER_SECOND} second")
                    audioController?.muteMicrophone()
                    session.schedule(AUDIO_END_DELAY_MILLISECONDS) { endCall(session) }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(tag, "Audio playback failed: what=$what, extra=$extra")
                    audioController?.muteMicrophone()
                    session.schedule(AUDIO_END_DELAY_MILLISECONDS) { endCall(session) }
                    true
                }
                prepare()
                // The microphone is unmuted only for the duration of the recording.
                audioController?.unmuteMicrophone()
                start()
            }
            Log.d(tag, "Audio playback started")
        } catch (exception: Exception) {
            Log.e(tag, "Could not play audio", exception)
            audioController?.muteMicrophone()
            endCall(session)
        }
    }

    private fun endCall(session: CallSession) {
        // The microphone stays muted, so the caller never hears the room.
        audioController?.muteMicrophone()

        stopAudio()
        try {
            session.call.disconnect()
            Log.d(tag, "Call ended")
        } catch (exception: Exception) {
            Log.e(tag, "Could not end the call", exception)
        }

        notifyCallHandled(session)
    }

    /** Stops playback and cancels every pending delay when the call ends before the sequence does. */
    private fun finishSession(session: CallSession) {
        if (session.isFinished) return
        session.isFinished = true
        session.isEnding = true

        session.cancelScheduledTasks()
        stopAudio()
        sessions.remove(session.identifier)

        Log.d(tag, "Call from ${session.callerNumber} ended")
        notifyCallHandled(session)
    }

    /** Sends the notification once per call, whether the application or the caller ended it. */
    private fun notifyCallHandled(session: CallSession) {
        if (session.isHandled) return
        session.isHandled = true

        // Sent only once the call has been disconnected so the SMTP transaction never delays the ending.
        EmailNotifier.sendCallNotification(
            callerNumber = session.callerNumber,
            ringTimeMillis = session.ringTimeMillis,
            answerTimeMillis = session.answeredAtMillis,
            endTimeMillis = System.currentTimeMillis()
        )
    }

    private fun stopAudio() {
        mediaPlayer?.let { player ->
            runCatching { player.stop() }
            player.release()
        }
        mediaPlayer = null
        audioController?.restoreAfterPlayback()
        audioController = null
    }

    private fun callerNumber(call: Call): String =
        call.details.handle?.schemeSpecificPart?.replace("\\s".toRegex(), "") ?: ""

    private fun callIdentifier(call: Call): String =
        "${call.details.handle}:${call.details.creationTimeMillis}"

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
        const val ANSWER_SETTLE_MILLISECONDS = 1_500L
        const val AUDIO_END_DELAY_MILLISECONDS = 1_000L
        const val MICROPHONE_MUTE_REAPPLY_MILLISECONDS = 600L
    }
}
