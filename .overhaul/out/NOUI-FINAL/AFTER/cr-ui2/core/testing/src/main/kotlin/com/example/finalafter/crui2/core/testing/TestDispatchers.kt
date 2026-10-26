package com.example.finalafter.crui2.core.testing

import com.example.finalafter.crui2.core.common.coroutines.AppDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope

/**
 * The app's dispatchers with every role on [dispatcher]. Work a class moves to `io` or `default`
 * then runs on the test's scheduler, so `runCurrent` and `advanceUntilIdle` reach it and virtual
 * time covers its delays.
 */
fun testDispatchers(dispatcher: CoroutineDispatcher): AppDispatchers =
    AppDispatchers(io = dispatcher, default = dispatcher, main = dispatcher)

/** [testDispatchers] on a new [StandardTestDispatcher] that shares this test's scheduler. */
fun TestScope.testDispatchers(): AppDispatchers =
    testDispatchers(StandardTestDispatcher(testScheduler))
