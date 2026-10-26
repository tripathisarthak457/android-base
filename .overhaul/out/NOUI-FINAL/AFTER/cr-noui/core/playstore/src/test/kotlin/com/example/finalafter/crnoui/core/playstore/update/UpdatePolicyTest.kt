package com.example.finalafter.crnoui.core.playstore.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePolicyTest {

    @Test
    fun `an update nobody turned down is offered`() {
        assertTrue(shouldOffer(availableVersionCode = 42, dismissedVersionCode = null))
    }

    @Test
    fun `the version turned down is not offered again`() {
        assertFalse(shouldOffer(availableVersionCode = 42, dismissedVersionCode = 42))
    }

    @Test
    fun `a newer version than the one turned down is offered`() {
        assertTrue(shouldOffer(availableVersionCode = 43, dismissedVersionCode = 42))
    }
}
