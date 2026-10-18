package com.example.wp04.abstd.core.navigation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.wp04.abstd.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** An activity's host, reduced to its lifecycle and the commands it applied. */
private class Host(state: Lifecycle.State) : LifecycleOwner {
    override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = state }
    val applied = mutableListOf<NavCommand>()
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppNavigatorTest {

    @get:Rule
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    private val navigator = AppNavigator()

    @Test
    fun `an activity stopped behind the one on screen takes none of its commands`() = runTest(main.testDispatcher) {
        val behind = Host(Lifecycle.State.CREATED)
        val front = Host(Lifecycle.State.RESUMED)
        backgroundScope.launch { navigator.applyWhileStarted(behind.lifecycle) { behind.applied += it } }
        backgroundScope.launch { navigator.applyWhileStarted(front.lifecycle) { front.applied += it } }

        repeat(4) { navigator.navigate(Post(it)) }

        assertEquals((0 until 4).map { NavCommand.Navigate(Post(it)) }, front.applied)
        assertEquals(emptyList<NavCommand>(), behind.applied)
    }

    @Test
    fun `a command sent while the app is in the background waits for it to come back`() = runTest(main.testDispatcher) {
        val host = Host(Lifecycle.State.CREATED)
        backgroundScope.launch { navigator.applyWhileStarted(host.lifecycle) { host.applied += it } }

        navigator.navigate(Post(1))
        assertEquals(emptyList<NavCommand>(), host.applied)

        host.lifecycle.currentState = Lifecycle.State.STARTED
        assertEquals(listOf(NavCommand.Navigate(Post(1))), host.applied)
    }
}
