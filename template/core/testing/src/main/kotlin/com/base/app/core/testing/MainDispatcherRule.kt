package com.base.app.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Swaps `Dispatchers.Main` for a test dispatcher around each test.
 *
 * Every `MviViewModel` launches its event loop in `viewModelScope`, which is hardwired to
 * `Dispatchers.Main`. Off-device there is no main looper, so without this the loop never runs and
 * every ViewModel test hangs or fails with "Module with the Main dispatcher had failed to
 * initialize" — a message that says nothing about the actual cause.
 *
 * `StandardTestDispatcher` rather than `UnconfinedTestDispatcher`: the standard one queues
 * coroutines until the test advances the clock, which is what lets a test assert the state
 * *before* a load completes as well as after. Unconfined runs everything eagerly and quietly
 * makes intermediate states untestable.
 *
 * ```
 * @get:Rule
 * val mainDispatcherRule = MainDispatcherRule()
 * ```
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
