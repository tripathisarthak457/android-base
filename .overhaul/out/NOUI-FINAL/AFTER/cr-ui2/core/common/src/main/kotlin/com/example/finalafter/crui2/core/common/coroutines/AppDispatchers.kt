package com.example.finalafter.crui2.core.common.coroutines

import kotlinx.coroutines.CoroutineDispatcher

/**
 * The dispatchers, injected as one value instead of referenced as `Dispatchers.IO` inside a class,
 * so a test hands every class its own test dispatcher. A plain class rather than three qualified
 * bindings: qualifier annotations come from `javax.inject`, which does not exist outside the JVM.
 */
class AppDispatchers(
    val io: CoroutineDispatcher,
    val default: CoroutineDispatcher,
    val main: CoroutineDispatcher,
)
