package ro.nucilandia.autocallresponder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class PhoneStateReceiver : BroadcastReceiver() {

    private val tag = "PhoneStateReceiver"
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var incomingNumber = ""

    companion object {
        private const val MILLIS_PER_SECOND = 1000L

        @Volatile
        private var lastState = TelephonyManager.CALL_STATE_IDLE
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getIntExtra(TelephonyManager.EXTRA_STATE, TelephonyManager.CALL_STATE_IDLE)
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""

        Log.d(tag, "Phone state changed: $state, number: $number")

        if (state != lastState) {
            lastState = state

            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    // While this app is the default dialer, AutoCallInCallService handles the call
                    // and drives the whole answer/playback sequence. Running this legacy path too
                    // would answer the call twice and start two audio players.
                    if (isDefaultDialer(context)) {
                        Log.d(tag, "Handled by AutoCallInCallService, ignoring broadcast")
                        return
                    }

                    incomingNumber = number.replace("\\s".toRegex(), "")
                    Log.d(tag, "Incoming call from: $incomingNumber")

                    // Check settings
                    val settings = SettingsManager.getSettings()
                    if (!settings.enabled) return

                    if (settings.filterRomanianNumbers && !RomanianPhoneNumbers.isRomanian(incomingNumber)) {
                        Log.d(tag, "Number is not Romanian, ignoring")
                        return
                    }

                    // Start handling after delay
                    val delayMilliseconds = settings.delaySeconds * MILLIS_PER_SECOND
                    handler.postDelayed({
                        startCallHandling(context, incomingNumber)
                    }, delayMilliseconds)
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    Log.d(tag, "Call answered/offhook")
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    Log.d(tag, "Call ended/idle")

                    // The call ended, so the pending start and any playback must stop at once. The
                    // service still sends the notification, including when it never began handling.
                    handler.removeCallbacksAndMessages(null)
                    stopCallHandling(context, incomingNumber)
                    incomingNumber = ""
                }
            }
        }
    }

    private fun isDefaultDialer(context: Context): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) return false
        val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
            ?: return false
        return roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_DIALER)
    }

    private fun startCallHandling(context: Context, callerNumber: String) {
        val intent = Intent(context, CallHandlingService::class.java).apply {
            action = CallHandlingService.ACTION_START
            putExtra(CallHandlingService.EXTRA_CALLER_NUMBER, callerNumber)
        }
        context.startForegroundService(intent)
    }

    /**
     * Stops the handling of a call that ended.
     *
     * The caller number is forwarded so that a call which ended during the pre-answer delay still
     * produces a notification, even though the service never began handling it.
     */
    private fun stopCallHandling(context: Context, callerNumber: String) {
        if (isDefaultDialer(context)) return

        val intent = Intent(context, CallHandlingService::class.java).apply {
            action = CallHandlingService.ACTION_STOP
            putExtra(CallHandlingService.EXTRA_CALLER_NUMBER, callerNumber)
        }
        try {
            context.startForegroundService(intent)
        } catch (exception: Exception) {
            Log.w(tag, "Could not stop the call handling service", exception)
        }
    }
}
