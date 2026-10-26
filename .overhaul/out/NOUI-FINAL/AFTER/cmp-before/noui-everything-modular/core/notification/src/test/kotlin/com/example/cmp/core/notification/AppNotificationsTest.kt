package com.example.cmp.core.notification

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.example.cmp.core.notification.channel.NotificationChannelSpec
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
class AppNotificationsTest {

    private val context: Application = RuntimeEnvironment.getApplication()
    private val posted = shadowOf(context.getSystemService(NotificationManager::class.java))
    private val notifications = AppNotifications(context)

    @Test
    fun `without the permission nothing is posted`() {
        notifications.post(id = 1, title = "Order shipped", body = "It arrives on Friday.")

        assertTrue(posted.allNotifications.isEmpty())
    }

    @Test
    fun `a notification carries its title, its whole body and its channel`() {
        grant()

        notifications.post(
            id = 1,
            title = "Order shipped",
            body = "It arrives on Friday.",
            channel = NotificationChannelSpec.Urgent,
        )

        val notification = posted.getNotification(1)
        assertEquals("Order shipped", notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals("It arrives on Friday.", notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString())
        assertEquals(NotificationChannelSpec.Urgent.id, notification.channelId)
    }

    // A message's text is nobody else's business, so the lock screen gets the public version.
    @Test
    fun `on the lock screen only a neutral line is shown`() {
        grant()

        notifications.post(id = 1, title = "From Ada", body = "The test results are in.")

        val notification = posted.getNotification(1)
        assertEquals(NotificationCompat.VISIBILITY_PRIVATE, notification.visibility)
        assertEquals(
            context.getString(R.string.notification_public_text),
            notification.publicVersion.extras.getCharSequence(Notification.EXTRA_TEXT).toString(),
        )
    }

    // Explicit, so no other app can answer the tap and receive the link.
    @Test
    fun `a tap opens the link in this app and no other`() {
        grant()
        val link = Uri.parse("https://example.com/orders/7")

        notifications.post(id = 1, title = "Order shipped", body = "It arrives on Friday.", link = link)

        val tap = shadowOf(posted.getNotification(1).contentIntent).savedIntent
        assertEquals(Intent.ACTION_VIEW, tap.action)
        assertEquals(link, tap.data)
        assertEquals(context.packageName, tap.`package`)
    }

    @Test
    fun `cancelling removes the notification`() {
        grant()
        notifications.post(id = 1, title = "Order shipped", body = "It arrives on Friday.")

        notifications.cancel(1)

        assertNull(posted.getNotification(1))
    }

    private fun grant() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }
}
