package ro.nucilandia.autocallresponder

import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log

/**
 * Restores the responder after a reboot.
 *
 * No service needs to be started: Telecom binds [CallScreeningService] and
 * [AutoCallInCallService] on its own as long as this app still holds the dialer and
 * call-screening roles. The receiver therefore verifies that those prerequisites survived the
 * reboot and warms up settings so the first incoming call is handled immediately.
 */
class BootReceiver : BroadcastReceiver() {

    private val tag = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(tag, "Received $action")

        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        SettingsManager.init(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            val holdsDialerRole = roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true
            val holdsScreeningRole = roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true

            if (!holdsDialerRole) {
                Log.w(tag, "Dialer role is not held; calls cannot be answered automatically")
            }
            if (!holdsScreeningRole) {
                Log.w(tag, "Call-screening role is not held; incoming calls are not observed")
            }
        }

        val powerManager = context.getSystemService(PowerManager::class.java)
        if (powerManager?.isIgnoringBatteryOptimizations(context.packageName) != true) {
            Log.w(tag, "Battery optimisation is enabled; the system may delay call handling")
        }

        if (SettingsManager.getSettings().enabled) {
            Log.i(tag, "Auto Call Responder is enabled and ready for incoming calls")

            // Start detection early so the root state is cached before the first call arrives.
            RootShell.detectAsync()
        } else {
            Log.i(tag, "Auto Call Responder is disabled in settings")
        }
    }
}
