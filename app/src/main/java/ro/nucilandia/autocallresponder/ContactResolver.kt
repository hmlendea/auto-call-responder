package ro.nucilandia.autocallresponder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log

/** Contact details attached to a caller number, when the number is stored in the contacts list. */
data class CallerContact(
    val displayName: String,
    val numberType: String?
)

/**
 * Resolves the contacts-list entry of a caller number.
 *
 * The lookup is best-effort: it returns `null` when the contacts permission is missing, when the
 * number is blank, when the number is not stored in the contacts list, or when the query fails.
 * It performs a content-provider query, so it must be called off the main thread.
 */
object ContactResolver {

    private const val TAG = "ContactResolver"
    private val SEPARATORS = "[\\s\\-()./]".toRegex()

    /** Returns the contact stored for [rawNumber], or `null` when it cannot be resolved. */
    fun resolve(context: Context, rawNumber: String): CallerContact? {
        val number = normalize(rawNumber)
        if (number.isEmpty()) return null

        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Contacts permission not granted; skipping the contact lookup")
            return null
        }

        return try {
            queryPhoneLookup(context, number)
        } catch (exception: SecurityException) {
            // The permission can be revoked while the application is running.
            Log.w(TAG, "Contacts permission was revoked; skipping the contact lookup", exception)
            null
        } catch (exception: Exception) {
            Log.w(TAG, "Could not resolve the caller contact", exception)
            null
        }
    }

    private fun normalize(rawNumber: String): String =
        rawNumber.removePrefix("tel:").replace(SEPARATORS, "").trim()

    private fun queryPhoneLookup(context: Context, number: String): CallerContact? {
        val lookupUri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.TYPE,
            ContactsContract.PhoneLookup.LABEL
        )

        context.contentResolver.query(lookupUri, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                val displayName = if (nameIndex < 0) null else cursor.getStringOrNull(nameIndex)
                if (displayName.isNullOrBlank()) continue

                val typeIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.TYPE)
                val labelIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.LABEL)
                val phoneType = if (typeIndex < 0) null else cursor.readPhoneType(typeIndex)
                val customLabel = if (labelIndex < 0) null else cursor.getStringOrNull(labelIndex)

                return CallerContact(
                    displayName = displayName.trim(),
                    numberType = resolveTypeLabel(context, phoneType, customLabel)
                )
            }
        }

        return null
    }

    /** Maps a [ContactsContract.CommonDataKinds.Phone] type to its localised label. */
    private fun resolveTypeLabel(context: Context, phoneType: Int?, customLabel: String?): String? {
        if (phoneType == null) return customLabel?.trim()?.takeIf { it.isNotEmpty() }

        val label = ContactsContract.CommonDataKinds.Phone
            .getTypeLabel(context.resources, phoneType, customLabel)
            .toString()
            .trim()

        return label.takeIf { it.isNotEmpty() }
    }

    /** Reads a nullable string column. */
    private fun Cursor.getStringOrNull(index: Int): String? =
        if (isNull(index)) null else getString(index)

    /**
     * Reads the phone type column, which is an integer for ordinary contacts but can be a string
     * for enterprise contacts.
     */
    private fun Cursor.readPhoneType(index: Int): Int? =
        when (getType(index)) {
            Cursor.FIELD_TYPE_INTEGER -> getInt(index)
            Cursor.FIELD_TYPE_STRING -> getString(index)?.toIntOrNull()
            else -> null
        }
}