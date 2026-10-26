package com.example.finalafter.crnoui3.core.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.finalafter.crnoui3.core.common.log.AppLogger
import com.example.finalafter.crnoui3.core.common.log.Log
import com.example.finalafter.crnoui3.core.mvi.MessageKind
import com.example.finalafter.crnoui3.core.mvi.UiMessage
import com.example.finalafter.crnoui3.core.mvi.UiText
import com.example.finalafter.crnoui3.core.testing.compose.TEST_SDK
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class MessageHostTest {

    @get:Rule
    val rule = createComposeRule()

    private val logged = mutableListOf<String>()

    @Before
    fun recordTheLog() {
        Log.logger = AppLogger { _, tag, message, _ -> if (tag == "Message") logged += message }
    }

    @After
    fun silenceTheLog() {
        Log.logger = null
    }

    @Test
    fun `without a presenter every message is taken and logged, none dropped`() {
        val messages = Channel<UiMessage>(Channel.BUFFERED)
        repeat(10) {
            messages.trySend(UiMessage(UiText.Dynamic("Message $it"), kind = MessageKind.Error))
        }

        rule.setContent { MessageHost(messages.receiveAsFlow()) }
        rule.waitForIdle()

        assertEquals(List(10) { "Error: Message $it" }, logged)
    }

    @Test
    fun `a presenter in scope is handed each message instead`() {
        val shown = mutableListOf<String>()
        val messages = Channel<UiMessage>(Channel.BUFFERED)
        messages.trySend(UiMessage(UiText.Dynamic("Saved"), title = UiText.Dynamic("Profile")))

        rule.setContent {
            CompositionLocalProvider(
                LocalMessagePresenter provides
                    MessagePresenter { shown += "${it.title}: ${it.text}" },
            ) {
                MessageHost(messages.receiveAsFlow())
            }
        }
        rule.waitForIdle()

        assertEquals(listOf("Profile: Saved"), shown)
        assertEquals(emptyList<String>(), logged)
    }
}
