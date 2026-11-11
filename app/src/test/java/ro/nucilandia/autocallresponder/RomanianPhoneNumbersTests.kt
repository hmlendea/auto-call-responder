package ro.nucilandia.autocallresponder

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies the Romanian number detection for fixed-line, mobile and foreign numbers. */
class RomanianPhoneNumbersTests {

    @Test
    fun `fixed-line number in national form is Romanian`() {
        assertTrue(RomanianPhoneNumbers.isRomanian("0376509045"))
        assertTrue(RomanianPhoneNumbers.isRomanian("0212345678"))
        assertTrue(RomanianPhoneNumbers.isRomanian("0234567890"))
    }

    @Test
    fun `fixed-line number in international form is Romanian`() {
        assertTrue(RomanianPhoneNumbers.isRomanian("+40376509045"))
        assertTrue(RomanianPhoneNumbers.isRomanian("0040376509045"))
        assertTrue(RomanianPhoneNumbers.isRomanian("40376509045"))
    }

    @Test
    fun `mobile number in national form is Romanian`() {
        assertTrue(RomanianPhoneNumbers.isRomanian("0722512537"))
        assertTrue(RomanianPhoneNumbers.isRomanian("0312345678"))
    }

    @Test
    fun `mobile number in international form is Romanian`() {
        assertTrue(RomanianPhoneNumbers.isRomanian("+40722512537"))
        assertTrue(RomanianPhoneNumbers.isRomanian("0040722512537"))
    }

    @Test
    fun `formatted numbers are Romanian`() {
        assertTrue(RomanianPhoneNumbers.isRomanian("0722 512 537"))
        assertTrue(RomanianPhoneNumbers.isRomanian("+40 (722) 512-537"))
        assertTrue(RomanianPhoneNumbers.isRomanian("tel:+40376509045"))
    }

    @Test
    fun `foreign numbers are not Romanian`() {
        assertFalse(RomanianPhoneNumbers.isRomanian("+37360000000"))
        assertFalse(RomanianPhoneNumbers.isRomanian("0012125551234"))
        assertFalse(RomanianPhoneNumbers.isRomanian("0612345678"))
    }

    @Test
    fun `incomplete and empty numbers are not Romanian`() {
        assertFalse(RomanianPhoneNumbers.isRomanian(""))
        assertFalse(RomanianPhoneNumbers.isRomanian("07"))
        assertFalse(RomanianPhoneNumbers.isRomanian("037650904"))
        assertFalse(RomanianPhoneNumbers.isRomanian("+40"))
    }
}
