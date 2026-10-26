package com.example.finalafter.crnoui3.core.common.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidatorsTest {

    @Test
    fun `required refuses blank text`() {
        assertEquals(ValidationError.Required, Validators.required().validate("   "))
        assertNull(Validators.required().validate("a"))
    }

    @Test
    fun `email is permissive but needs a domain with a dot`() {
        assertNull(Validators.email().validate(" ada+notes@example.co.uk "))
        assertEquals(ValidationError.Email, Validators.email().validate("ada@example"))
        assertEquals(ValidationError.Email, Validators.email().validate("ada example.com"))
    }

    @Test
    fun `minLength counts the trimmed text and reports the minimum`() {
        assertEquals(ValidationError.TooShort(2), Validators.minLength(2).validate(" a "))
        assertNull(Validators.minLength(2).validate("ab"))
    }

    @Test
    fun `password reports the first unmet requirement`() {
        val rule = Validators.password(minLength = 8)

        assertEquals(ValidationError.PasswordTooShort(8), rule.validate("a1"))
        assertEquals(ValidationError.PasswordNeedsLetter, rule.validate("12345678"))
        assertEquals(ValidationError.PasswordNeedsDigit, rule.validate("abcdefgh"))
        assertNull(rule.validate("abcdefg1"))
    }

    @Test
    fun `sameAs compares with the other value at the moment of checking`() {
        var password = "first1234"
        val rule = Validators.sameAs { password }

        assertNull(rule.validate("first1234"))
        password = "second1234"
        assertEquals(ValidationError.Mismatch, rule.validate("first1234"))
    }

    @Test
    fun `and reports the first rule that fails`() {
        val rule = Validators.required() and Validators.email()

        assertEquals(ValidationError.Required, rule.validate(""))
        assertEquals(ValidationError.Email, rule.validate("nope"))
    }
}
