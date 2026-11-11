package ro.nucilandia.autocallresponder

/** Detects Romanian telephone numbers in national and international formats. */
object RomanianPhoneNumbers {

    private val SEPARATORS = "[\\s\\-()./]".toRegex()
    private val NATIONAL_PATTERN = Regex("^0[237]\\d{8}$")
    private val COUNTRY_CODE = "40"
    private val INTERNATIONAL_PREFIX = "00"

    /**
     * Returns true when [rawNumber] is a Romanian fixed-line or mobile number,
     * in national form (`0212345678`, `0722512537`), international form
     * (`+40212345678`, `+40722512537`) or international dialling form
     * (`0040212345678`, `0040722512537`).
     */
    fun isRomanian(rawNumber: String): Boolean {
        val normalizedNumber = normalize(rawNumber)
        if (normalizedNumber.isEmpty()) return false

        val nationalNumber = toNationalForm(normalizedNumber)
        return NATIONAL_PATTERN.matches(nationalNumber)
    }

    private fun normalize(rawNumber: String): String =
        rawNumber.removePrefix("tel:").replace(SEPARATORS, "").trim()

    private fun toNationalForm(normalizedNumber: String): String = when {
        normalizedNumber.startsWith("+$COUNTRY_CODE") ->
            toNationalNumber(normalizedNumber.removePrefix("+$COUNTRY_CODE"))
        normalizedNumber.startsWith(INTERNATIONAL_PREFIX + COUNTRY_CODE) ->
            toNationalNumber(normalizedNumber.removePrefix(INTERNATIONAL_PREFIX + COUNTRY_CODE))
        normalizedNumber.startsWith(COUNTRY_CODE) ->
            toNationalNumber(normalizedNumber.removePrefix(COUNTRY_CODE))
        else -> normalizedNumber
    }

    private fun toNationalNumber(subscriberNumber: String): String = "0$subscriberNumber"
}
