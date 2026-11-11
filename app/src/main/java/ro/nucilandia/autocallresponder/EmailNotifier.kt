package ro.nucilandia.autocallresponder

import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Sends call notifications without opening a mail application. */
object EmailNotifier {

    private const val TAG = "EmailNotifier"

    /** Sends a notification for a handled call. Call this only after the call has been disconnected. */
    fun sendCallNotification(
        callerNumber: String,
        ringTimeMillis: Long,
        answerTimeMillis: Long,
        endTimeMillis: Long
    ) {
        send(
            callerNumber = callerNumber,
            subject = buildSubject(callerNumber),
            body = buildBody(callerNumber, ringTimeMillis, answerTimeMillis, endTimeMillis)
        )
    }

    private fun buildSubject(callerNumber: String): String =
        if (callerNumber.isBlank()) "Handled an unknown call" else "Handled a call from $callerNumber"

    private fun buildBody(
        callerNumber: String,
        ringTimeMillis: Long,
        answerTimeMillis: Long,
        endTimeMillis: Long
    ): String {
        val displayedNumber = callerNumber.ifBlank { "Unknown / private number" }
        val answerDelaySeconds = durationSeconds(ringTimeMillis, answerTimeMillis)
        val talkSeconds = durationSeconds(answerTimeMillis, endTimeMillis)
        val totalSeconds = durationSeconds(ringTimeMillis, endTimeMillis)
        val wasAnswered = answerTimeMillis >= ringTimeMillis && ringTimeMillis > 0L
        val settings = SettingsManager.getSettings()

        return buildString {
            appendLine("Hello,")
            appendLine()
            appendLine("The Auto Call Responder app handled an incoming call on your phone.")
            appendLine()
            appendLine("Call summary")
            appendLine("------------")
            appendLine("Caller number: $displayedNumber")
            appendLine("First ring: ${formatTimestamp(ringTimeMillis)}")
            if (wasAnswered) {
                appendLine("Answered at: ${formatTimestamp(answerTimeMillis)}")
            } else {
                appendLine("Answered at: not answered automatically")
            }
            appendLine("Call ended: ${formatTimestamp(endTimeMillis)}")
            if (wasAnswered) {
                appendLine("Answer delay: $answerDelaySeconds seconds")
                appendLine("Call duration after answer: $talkSeconds seconds")
            }
            appendLine("Total time: $totalSeconds seconds")
            appendLine()
            appendLine("Playback")
            appendLine("--------")
            appendLine("Audio file: ${resolveAudioDescription(settings)}")
            appendLine(
                "Output route: ${if (settings.earpieceOnly) "Earpiece (loudspeaker disabled)" else "Device default"}"
            )
            appendLine("Note: Android does not allow apps to inject audio into the call uplink, so the")
            appendLine("      other party could not hear this recording.")
            appendLine()
            appendLine("Settings at the time of the call")
            appendLine("--------------------------------")
            appendLine("Responder enabled: ${if (settings.enabled) "Yes" else "No"}")
            appendLine("Delay before answering: ${settings.delaySeconds} seconds")
            appendLine("Delay after answering before audio: ${settings.internalDelaySeconds} seconds")
            appendLine(
                "Romanian number filter: ${if (settings.filterRomanianNumbers) "Enabled" else "Disabled"}"
            )
            appendLine()
            appendLine("Device")
            appendLine("------")
            appendLine("Model: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine()
            appendLine("--")
            appendLine("Sent automatically by Auto Call Responder.")
        }
    }

    private fun resolveAudioDescription(settings: AppSettings): String {
        val path = settings.audioFilePath.takeIf { it.isNotBlank() }
            ?: SettingsManager.getAudioFileUri()
        return path ?: "No audio file configured"
    }

    private fun send(callerNumber: String, subject: String, body: String) {
        val settings = SettingsManager.getSettings()
        if (settings.emailAddress.isBlank() || settings.smtpUser.isBlank() || settings.smtpPassword.isBlank()) {
            Log.w(TAG, "Email settings are incomplete")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                EmailSender(
                    settings.smtpHost,
                    settings.smtpPort,
                    settings.smtpUser,
                    settings.smtpPassword
                ).send(
                    senderAddress = settings.smtpUser,
                    recipientAddress = settings.emailAddress,
                    subject = subject,
                    body = body
                )
                Log.d(TAG, "Call notification sent for $callerNumber")
            } catch (exception: Exception) {
                Log.e(TAG, "Could not send call notification", exception)
            }
        }
    }

    private fun durationSeconds(fromMillis: Long, toMillis: Long): Long {
        if (fromMillis <= 0L || toMillis < fromMillis) return 0L
        return TimeUnit.MILLISECONDS.toSeconds(toMillis - fromMillis)
    }

    private fun formatTimestamp(millis: Long): String {
        if (millis <= 0L) return "unknown"
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(millis))
    }
}
