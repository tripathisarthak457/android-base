package com.example.finalafter.crui2.core.notification.permission

import android.Manifest
import android.app.Application
import android.content.Intent
import android.provider.Settings
import com.example.finalafter.crui2.core.notification.ROBOLECTRIC_SDK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class NotificationPermissionTest {

    private val context: Application = RuntimeEnvironment.getApplication()
    private var questions = 0

    private fun permission() = NotificationPermission(context) { questions++ }

    @Test
    fun `not granted yet, a request asks the system`() {
        val permission = permission()

        permission.request()

        assertEquals(NotificationAccess.NotGranted, permission.access)
        assertEquals(1, questions)
        assertNull(shadowOf(context).nextStartedActivity)
    }

    @Test
    fun `once granted, a request does nothing`() {
        grant()
        val permission = permission()

        permission.request()

        assertEquals(NotificationAccess.Granted, permission.access)
        assertEquals(0, questions)
        assertNull(shadowOf(context).nextStartedActivity)
    }

    // The system no longer asks, so asking it again would do nothing the person could see.
    @Test
    fun `once blocked, a request opens this app's notification settings`() {
        val permission = permission()
        permission.access = NotificationAccess.Blocked

        permission.request()

        val opened = shadowOf(context).nextStartedActivity
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, opened.action)
        assertEquals(context.packageName, opened.getStringExtra(Settings.EXTRA_APP_PACKAGE))
        assertTrue(opened.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertEquals(0, questions)
    }

    @Test
    fun `a refusal for good stays blocked when the screen comes back`() {
        val permission = permission()
        permission.access = NotificationAccess.Blocked

        permission.refresh()

        assertEquals(NotificationAccess.Blocked, permission.access)
    }

    @Test
    fun `a grant made in the system's settings is seen when the screen comes back`() {
        val permission = permission()
        permission.access = NotificationAccess.Blocked

        grant()
        permission.refresh()

        assertEquals(NotificationAccess.Granted, permission.access)
    }

    @Test
    fun `a grant withdrawn in the system's settings is seen when the screen comes back`() {
        grant()
        val permission = permission()

        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        permission.refresh()

        assertEquals(NotificationAccess.NotGranted, permission.access)
    }

    private fun grant() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }
}
