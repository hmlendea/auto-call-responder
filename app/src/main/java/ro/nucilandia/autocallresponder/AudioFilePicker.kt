package ro.nucilandia.autocallresponder

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

/** Selects an audio file from any installed document provider and imports it into app storage. */
object AudioFilePicker {

    const val REQUEST_CODE = 1001
    private const val TAG = "AudioFilePicker"
    private const val MIME_TYPE = "audio/*"

    private const val IMPORTED_FILE_NAME = "selected_call_audio"
    private const val DEFAULT_FILE_EXTENSION = "mp3"

    /** Launches the first provider that accepts an audio-file request. */
    fun launch(activity: Activity) {
        for (intent in buildCandidates()) {
            try {
                activity.startActivityForResult(intent, REQUEST_CODE)
                return
            } catch (exception: ActivityNotFoundException) {
                // Package visibility can hide installed pickers from resolveActivity, so the
                // launch itself is the authoritative availability check.
                Log.w(TAG, "No handler for ${intent.action} with type ${intent.type}, trying the next option")
            } catch (exception: Exception) {
                Log.e(TAG, "Could not open a picker for ${intent.action}", exception)
            }
        }

        Toast.makeText(activity, "No file manager available to select audio", Toast.LENGTH_LONG).show()
    }

    /** Copies the selected document into app storage and returns a stable URI for playback. */
    fun import(context: Context, sourceUri: Uri): Uri? {
        return try {
            val targetFile = createTargetFile(context, sourceUri)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                targetFile.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("The selected file could not be opened")

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", targetFile)
        } catch (exception: Exception) {
            Log.e(TAG, "Could not import the selected audio file", exception)
            Toast.makeText(context, "Could not read the selected audio file", Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun buildCandidates(): List<Intent> = listOf(
        Intent(Intent.ACTION_GET_CONTENT).apply {
            type = MIME_TYPE
            addCategory(Intent.CATEGORY_OPENABLE)
        },
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = MIME_TYPE
            addCategory(Intent.CATEGORY_OPENABLE)
        },
        Intent(Intent.ACTION_PICK).apply {
            type = MIME_TYPE
        }
    )

    private fun createTargetFile(context: Context, sourceUri: Uri): File {
        val storageDirectory = File(context.filesDir, AUDIO_DIRECTORY_NAME).apply { mkdirs() }
        storageDirectory.listFiles()?.forEach { existingFile -> existingFile.delete() }

        val extension = resolveExtension(context, sourceUri)
        return File(storageDirectory, "$IMPORTED_FILE_NAME.$extension")
    }

    private fun resolveExtension(context: Context, sourceUri: Uri): String {
        val mimeType = context.contentResolver.getType(sourceUri) ?: return DEFAULT_FILE_EXTENSION
        val subtype = mimeType.substringAfter('/', DEFAULT_FILE_EXTENSION)
        return subtype.filter { character -> character.isLetterOrDigit() }.ifBlank { DEFAULT_FILE_EXTENSION }
    }

    private const val AUDIO_DIRECTORY_NAME = "call_audio"
}
