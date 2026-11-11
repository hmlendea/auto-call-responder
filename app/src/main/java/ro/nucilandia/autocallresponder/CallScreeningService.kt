package ro.nucilandia.autocallresponder

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log

/** Empty CallResponse = do not disallow, do not reject, do not silence. */
private val ALLOW_RESPONSE: CallScreeningService.CallResponse =
    CallScreeningService.CallResponse.Builder().build()

/**
 * Observes incoming calls while the app holds the call-screening role.
 *
 * A screening service may only allow, reject or silence a call, and Telecom unbinds it as soon as
 * [onScreenCall] returns. It therefore never answers or plays audio itself: the actual handling is
 * performed by [AutoCallInCallService], which stays bound for the whole call while this app is the
 * default dialer.
 */
class CallScreeningService : CallScreeningService() {

    private val tag = "CallScreeningService"

    override fun onScreenCall(callDetails: Call.Details) {
        val rawHandle = callDetails.handle?.schemeSpecificPart ?: callDetails.handle?.toString() ?: ""
        val callerNumber = rawHandle.removePrefix("tel:").replace("\\s".toRegex(), "")
        Log.d(tag, "onScreenCall handle: $rawHandle, number: $callerNumber")

        // Always allow the call so it keeps ringing; the answer happens after the configured delay.
        respondToCall(callDetails, ALLOW_RESPONSE)

        val settings = SettingsManager.getSettings()
        if (!settings.enabled) {
            Log.d(tag, "Responder disabled, allowing call normally")
            return
        }

        if (settings.filterRomanianNumbers && !RomanianPhoneNumbers.isRomanian(callerNumber)) {
            Log.d(tag, "Number is not Romanian, allowing call normally: $callerNumber")
            return
        }

        Log.d(tag, "Call from $callerNumber will be handled by AutoCallInCallService")
    }
}
