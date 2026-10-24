package com.base.app.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * [runTest] with `Dispatchers.Main` swapped for a dispatcher on the test's own scheduler, so work a
 * ViewModel starts in `viewModelScope` runs when the test calls `runCurrent()` or
 * `advanceUntilIdle()`, and virtual time covers its delays. The common-code counterpart of the
 * Android template's `MainDispatcherRule`, which needs JUnit.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun runViewModelTest(testBody: suspend TestScope.() -> Unit): TestResult {
    val main = StandardTestDispatcher()
    Dispatchers.setMain(main)
    try {
        return runTest(main) { testBody() }
    } finally {
        Dispatchers.resetMain()
    }
}
