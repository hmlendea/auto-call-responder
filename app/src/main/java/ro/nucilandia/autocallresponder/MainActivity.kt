package ro.nucilandia.autocallresponder

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private val REQUEST_CODE_PERMISSIONS = 1002
    private val REQUEST_CODE_CALL_SCREENING = 1003
    private val DONATION_URL = "https://hmlendea.go.ro/funding"

    private lateinit var tvStatus: TextView
    private lateinit var tvDelay: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvAudioFile: TextView
    private lateinit var swEnabled: Switch
    private lateinit var btnSettings: Button
    private lateinit var btnSelectAudio: Button
    private lateinit var btnTestEmail: Button
    private lateinit var btnRequestPermissions: Button
    private lateinit var btnDonate: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        updateUI()
        checkPermissions()
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvDelay = findViewById(R.id.tvDelay)
        tvEmail = findViewById(R.id.tvEmail)
        tvAudioFile = findViewById(R.id.tvAudioFile)
        swEnabled = findViewById(R.id.swEnabled)
        btnSettings = findViewById(R.id.btnSettings)
        btnSelectAudio = findViewById(R.id.btnSelectAudio)
        btnTestEmail = findViewById(R.id.btnTestEmail)
        btnRequestPermissions = findViewById(R.id.btnRequestPermissions)
        btnDonate = findViewById(R.id.btnDonate)
    }

    private fun setupListeners() {
        swEnabled.setOnCheckedChangeListener { _, isChecked ->
            val settings = SettingsManager.getSettings().apply { enabled = isChecked }
            SettingsManager.saveSettings(settings)
            updateUI()
            Toast.makeText(this, if (isChecked) "Enabled" else "Disabled", Toast.LENGTH_SHORT).show()
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnSelectAudio.setOnClickListener {
            pickAudioFile()
        }

        btnTestEmail.setOnClickListener {
            sendTestEmail()
        }

        btnRequestPermissions.setOnClickListener {
            requestPermissions()
        }

        btnDonate.setOnClickListener {
            openDonationPage()
        }
    }

    private fun openDonationPage() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(DONATION_URL)))
        } catch (exception: Exception) {
            Log.w(TAG, "Could not open the donation page", exception)
            Toast.makeText(this, "Could not open the donation page", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateUI() {
        val settings = SettingsManager.getSettings()
        swEnabled.isChecked = settings.enabled

        tvStatus.text = if (settings.enabled) "Status: Enabled" else "Status: Disabled"
        tvStatus.setTextColor(ContextCompat.getColor(this, if (settings.enabled) R.color.green else R.color.red))

        tvDelay.text = "Delay: ${settings.delaySeconds} seconds"
        tvEmail.text = "Email: ${if (settings.emailAddress.isNotBlank()) settings.emailAddress else "Not set"}"
        tvAudioFile.text = "Audio: ${if (settings.audioFilePath.isNotBlank() || SettingsManager.getAudioFileUri() != null) "Selected" else "Not selected"}"
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        // Check the dialer role (Android 10+), required to answer calls
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasDialerRole()) {
            btnRequestPermissions.visibility = android.view.View.VISIBLE
        }

        // Check other permissions
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.READ_PHONE_STATE)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ANSWER_PHONE_CALLS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.ANSWER_PHONE_CALLS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.RECORD_AUDIO)
        }

        if (permissions.isNotEmpty()) {
            btnRequestPermissions.visibility = android.view.View.VISIBLE
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.READ_PHONE_STATE)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ANSWER_PHONE_CALLS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.ANSWER_PHONE_CALLS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.RECORD_AUDIO)
        }

        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        }

        // Request the dialer role (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasDialerRole()) {
            requestDialerRole()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasCallScreeningRole()) {
            requestCallScreeningRole()
        }
    }

    private fun hasCallScreeningRole(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val roleManager = getSystemService(android.app.role.RoleManager::class.java)
        return roleManager?.isRoleHeld(android.app.role.RoleManager.ROLE_CALL_SCREENING) == true
    }

    private fun requestCallScreeningRole() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        try {
            val roleManager = getSystemService(android.app.role.RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_CALL_SCREENING)) {
                val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_CALL_SCREENING)
                startActivityForResult(intent, REQUEST_CODE_CALL_SCREENING)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not request the call-screening role", e)
        }
    }

    private fun hasDialerRole(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val roleManager = getSystemService(android.app.role.RoleManager::class.java)
        return roleManager?.isRoleHeld(android.app.role.RoleManager.ROLE_DIALER) == true
    }

    private fun requestDialerRole() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        try {
            val roleManager = getSystemService(android.app.role.RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_DIALER)) {
                val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_DIALER)
                startActivityForResult(intent, REQUEST_CODE_CALL_SCREENING)
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not request the dialer role", e)
        }

        try {
            startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        } catch (e: Exception) {
            Log.w(TAG, "Could not open the default apps settings", e)
        }
    }

    private fun pickAudioFile() {
        AudioFilePicker.launch(this)
    }

    private fun sendTestEmail() {
        val settings = SettingsManager.getSettings()
        if (settings.emailAddress.isBlank()) {
            Toast.makeText(this, "Email address not configured", Toast.LENGTH_SHORT).show()
            return
        }

        if (settings.smtpUser.isBlank() || settings.smtpPassword.isBlank()) {
            Toast.makeText(this, "SMTP user and password required", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(this, "Sending test email...", Toast.LENGTH_SHORT).show()
        btnTestEmail.isEnabled = false

        lifecycleScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    sendTestEmailMessage(settings)
                }
            }

            btnTestEmail.isEnabled = true

            result.onSuccess {
                Toast.makeText(this@MainActivity, "Test email sent", Toast.LENGTH_SHORT).show()
            }.onFailure { exception ->
                Log.e(TAG, "Could not send test email", exception)
                val message = exception.message ?: exception.javaClass.simpleName
                Toast.makeText(this@MainActivity, "Email failed: $message", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendTestEmailMessage(settings: AppSettings) {
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val body = "This is a test email from Auto Call Responder.\n\nTime: $timestamp"

        EmailSender(
            host = settings.smtpHost,
            port = settings.smtpPort,
            username = settings.smtpUser,
            password = settings.smtpPassword
        ).send(
            senderAddress = settings.smtpUser,
            recipientAddress = settings.emailAddress,
            subject = "Test email from Auto Call Responder",
            body = body
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                AudioFilePicker.REQUEST_CODE -> {
                    data.data?.let { uri -> storeAudioFile(uri) }
                }
            }
        }
    }

    private fun storeAudioFile(sourceUri: android.net.Uri) {
        val importedUri = AudioFilePicker.import(this, sourceUri) ?: return

        val settings = SettingsManager.getSettings().apply { audioFilePath = importedUri.toString() }
        SettingsManager.saveSettings(settings)
        SettingsManager.saveAudioFileUri(importedUri.toString())
        updateUI()
        Toast.makeText(this, "Audio file selected", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            val allGranted = grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
                btnRequestPermissions.visibility = android.view.View.GONE
            } else {
                Toast.makeText(this, "Some permissions denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }
}
