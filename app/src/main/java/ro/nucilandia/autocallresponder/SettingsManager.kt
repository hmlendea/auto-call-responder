package ro.nucilandia.autocallresponder

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class AppSettings(
    var delaySeconds: Int = 3,
    var internalDelaySeconds: Int = 2,
    var earpieceOnly: Boolean = true,
    var emailAddress: String = "",
    var smtpHost: String = "smtp.gmail.com",
    var smtpPort: Int = 587,
    var smtpUser: String = "",
    var smtpPassword: String = "",
    var audioFilePath: String = "",
    var enabled: Boolean = true,
    var filterRomanianNumbers: Boolean = true
)

object SettingsManager {
    private const val PREFS_NAME = "AutoCallResponderPrefs"
    private const val KEY_SETTINGS = "settings"
    private const val KEY_AUDIO_FILE_URI = "audio_file_uri"

    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSettings(): AppSettings {
        val json = sharedPreferences.getString(KEY_SETTINGS, null)
        return if (json != null) {
            gson.fromJson(json, AppSettings::class.java)
        } else {
            AppSettings()
        }
    }

    fun saveSettings(settings: AppSettings) {
        val json = gson.toJson(settings)
        sharedPreferences.edit().putString(KEY_SETTINGS, json).apply()
    }

    fun getAudioFileUri(): String? {
        return sharedPreferences.getString(KEY_AUDIO_FILE_URI, null)
    }

    fun saveAudioFileUri(uri: String) {
        sharedPreferences.edit().putString(KEY_AUDIO_FILE_URI, uri).apply()
    }

    fun clearAudioFileUri() {
        sharedPreferences.edit().remove(KEY_AUDIO_FILE_URI).apply()
    }
}
