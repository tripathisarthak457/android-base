package com.example.finalafter.crnoui.core.notification.channel

import android.app.Application
import android.app.NotificationManager
import com.example.finalafter.crnoui.core.notification.ROBOLECTRIC_SDK
import com.example.finalafter.crnoui.core.testing.testDispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class NotificationChannelsTest {

    private val context: Application = RuntimeEnvironment.getApplication()
    private val manager = context.getSystemService(NotificationManager::class.java)

    @Test
    fun `every channel is created with its name, description and importance`() = runTest {
        NotificationChannels(context, testDispatchers()).create()

        NotificationChannelSpec.entries.forEach { spec ->
            val channel = manager.getNotificationChannel(spec.id)
            assertEquals(spec.id, context.getString(spec.channelName), channel.name)
            assertEquals(spec.id, context.getString(spec.description), channel.description)
            assertEquals(spec.id, spec.importance, channel.importance)
        }
    }

    // It runs at every start, so a second run must update the channels, never add to them.
    @Test
    fun `creating again at the next start leaves one channel each`() = runTest {
        val channels = NotificationChannels(context, testDispatchers())

        channels.create()
        channels.create()

        assertEquals(
            NotificationChannelSpec.entries.map { it.id }.sorted(),
            manager.notificationChannels.map { it.id }.sorted(),
        )
    }

    // A person mutes a channel by its id: two specs sharing one would mute each other.
    @Test
    fun `every channel has an id of its own`() {
        val ids = NotificationChannelSpec.entries.map { it.id }

        assertEquals(ids.distinct(), ids)
    }
}
