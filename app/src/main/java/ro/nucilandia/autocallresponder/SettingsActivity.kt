package ro.nucilandia.autocallresponder

import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class SettingsActivity : AppCompatActivity() {

    private val tag = "SettingsActivity"

    private lateinit var etDelay: EditText
    private lateinit var etInternalDelay: EditText
    private lateinit var etEmail: EditText
    private lateinit var etSmtpHost: EditText
    private lateinit var etSmtpPort: EditText
    private lateinit var etSmtpUser: EditText
    private lateinit var etSmtpPassword: EditText
    private lateinit var tvAudioFile: TextView
    private lateinit var swFilterRomanian: SwitchCompat
    private lateinit var swEarpiece: SwitchCompat
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnSelectAudio: Button
    private lateinit var btnClearAudio: Button
    private lateinit var seekBarDelay: SeekBar
    private lateinit var tvDelayValue: TextView
    private lateinit var tvRootStatus: TextView
    private lateinit var btnGrantRoot: Button
    private lateinit var btnBatteryExemption: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()
        loadSettings()
        setupListeners()
    }

    private fun initViews() {
        etDelay = findViewById(R.id.etDelay)
        etInternalDelay = findViewById(R.id.etInternalDelay)
        etEmail = findViewById(R.id.etEmail)
        etSmtpHost = findViewById(R.id.etSmtpHost)
        etSmtpPort = findViewById(R.id.etSmtpPort)
        etSmtpUser = findViewById(R.id.etSmtpUser)
        etSmtpPassword = findViewById(R.id.etSmtpPassword)
        tvAudioFile = findViewById(R.id.tvAudioFile)
        swFilterRomanian = findViewById(R.id.swFilterRomanian)
        swEarpiece = findViewById(R.id.swEarpiece)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        btnSelectAudio = findViewById(R.id.btnSelectAudio)
        btnClearAudio = findViewById(R.id.btnClearAudio)
        seekBarDelay = findViewById(R.id.seekBarDelay)
        tvDelayValue = findViewById(R.id.tvDelayValue)
        tvRootStatus = findViewById(R.id.tvRootStatus)
        btnGrantRoot = findViewById(R.id.btnGrantRoot)
        btnBatteryExemption = findViewById(R.id.btnBatteryExemption)
    }

    private fun loadSettings() {
        val settings = SettingsManager.getSettings()
        etDelay.setText(settings.delaySeconds.toString())
        etInternalDelay.setText(settings.internalDelaySeconds.toString())
        etEmail.setText(settings.emailAddress)
        etSmtpHost.setText(settings.smtpHost)
        etSmtpPort.setText(settings.smtpPort.toString())
        etSmtpUser.setText(settings.smtpUser)
        etSmtpPassword.setText(settings.smtpPassword)
        swFilterRomanian.isChecked = settings.filterRomanianNumbers
        swEarpiece.isChecked = settings.earpieceOnly

        val audioUri = SettingsManager.getAudioFileUri()
        tvAudioFile.text = if (audioUri != null) "Audio file selected" else "No audio file selected"

        seekBarDelay.progress = settings.delaySeconds
        tvDelayValue.text = "${settings.delaySeconds} seconds"

        RootShell.cachedAvailability?.let { granted -> updateRootStatus(granted) }
    }

    private fun setupListeners() {
        seekBarDelay.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvDelayValue.text = "$progress seconds"
                etDelay.setText(progress.toString())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnSelectAudio.setOnClickListener {
            AudioFilePicker.launch(this)
        }

        btnClearAudio.setOnClickListener {
            SettingsManager.clearAudioFileUri()
            val settings = SettingsManager.getSettings().apply { audioFilePath = "" }
            SettingsManager.saveSettings(settings)
            tvAudioFile.text = "No audio file selected"
        }

        btnSave.setOnClickListener {
            saveSettings()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnGrantRoot.setOnClickListener {
            requestRootAccess()
        }

        btnBatteryExemption.setOnClickListener {
            requestBatteryExemption()
        }
    }

    private fun requestRootAccess() {
        tvRootStatus.text = "Root: requesting permission..."
        btnGrantRoot.isEnabled = false

        RootShell.requestAccess { granted ->
            btnGrantRoot.isEnabled = true
            updateRootStatus(granted)

            Toast.makeText(
                this,
                if (granted) "Root access granted" else "Root access denied or unavailable",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun updateRootStatus(granted: Boolean) {
        tvRootStatus.text = when {
            granted -> "Root: granted (uplink routing available)"
            RootShell.cachedAvailability == false -> "Root: not granted (local playback only)"
            else -> "Root: not checked"
        }
    }

    private fun requestBatteryExemption() {
        try {
            val intent = android.content.Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = android.net.Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (exception: Exception) {
            Log.w(tag, "Could not request the battery optimisation exemption", exception)

            try {
                startActivity(android.content.Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (nestedException: Exception) {
                Toast.makeText(this, "Open the battery settings manually", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveSettings() {
        val settings = SettingsManager.getSettings()

        val delay = etDelay.text.toString().toIntOrNull() ?: 3
        settings.delaySeconds = delay.coerceIn(1, 60)

        val internalDelay = etInternalDelay.text.toString().toIntOrNull() ?: 2
        settings.internalDelaySeconds = internalDelay.coerceIn(0, 60)

        settings.earpieceOnly = swEarpiece.isChecked

        settings.emailAddress = etEmail.text.toString().trim()
        settings.smtpHost = etSmtpHost.text.toString().trim()
        settings.smtpPort = etSmtpPort.text.toString().toIntOrNull() ?: 587
        settings.smtpUser = etSmtpUser.text.toString().trim()
        settings.smtpPassword = etSmtpPassword.text.toString()
        settings.filterRomanianNumbers = swFilterRomanian.isChecked

        SettingsManager.saveSettings(settings)
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null && requestCode == AudioFilePicker.REQUEST_CODE) {
            data.data?.let { uri -> storeAudioFile(uri) }
        }
    }

    private fun storeAudioFile(sourceUri: android.net.Uri) {
        val importedUri = AudioFilePicker.import(this, sourceUri) ?: return

        val settings = SettingsManager.getSettings().apply { audioFilePath = importedUri.toString() }
        SettingsManager.saveSettings(settings)
        SettingsManager.saveAudioFileUri(importedUri.toString())
        tvAudioFile.text = "Audio file selected"
    }
}
