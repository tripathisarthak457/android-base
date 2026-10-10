package com.base.app.core.common.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ValidatorsTest {

    @Test
    fun required_refusesBlankText() {
        assertEquals(ValidationError.Required, Validators.required().validate("   "))
        assertNull(Validators.required().validate("a"))
    }

    @Test
    fun email_isPermissiveButNeedsDomainWithDot() {
        assertNull(Validators.email().validate(" ada+notes@example.co.uk "))
        assertEquals(ValidationError.Email, Validators.email().validate("ada@example"))
        assertEquals(ValidationError.Email, Validators.email().validate("ada example.com"))
    }

    @Test
    fun minLength_countsTrimmedText_reportsMinimum() {
        assertEquals(ValidationError.TooShort(2), Validators.minLength(2).validate(" a "))
        assertNull(Validators.minLength(2).validate("ab"))
    }

    @Test
    fun password_reportsFirstUnmetRequirement() {
        val rule = Validators.password(minLength = 8)

        assertEquals(ValidationError.PasswordTooShort(8), rule.validate("a1"))
        assertEquals(ValidationError.PasswordNeedsLetter, rule.validate("12345678"))
        assertEquals(ValidationError.PasswordNeedsDigit, rule.validate("abcdefgh"))
        assertNull(rule.validate("abcdefg1"))
    }

    @Test
    fun sameAs_comparesWithOtherValueAtTimeOfChecking() {
        var password = "first1234"
        val rule = Validators.sameAs { password }

        assertNull(rule.validate("first1234"))
        password = "second1234"
        assertEquals(ValidationError.Mismatch, rule.validate("first1234"))
    }

    @Test
    fun and_reportsFirstRuleThatFails() {
        val rule = Validators.required() and Validators.email()

        assertEquals(ValidationError.Required, rule.validate(""))
        assertEquals(ValidationError.Email, rule.validate("nope"))
    }
}
