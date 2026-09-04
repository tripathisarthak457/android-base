package com.base.app.core.coroutines

import javax.inject.Qualifier

/**
 * Dispatchers are injected, never referenced as `Dispatchers.IO` inside a class.
 *
 * A class that reaches for the global dispatcher cannot be tested without a real thread pool and
 * real time: `runTest`'s virtual clock only skips delays on the test dispatcher it controls, so
 * a hardcoded `Dispatchers.IO` turns a millisecond test into a real one-second wait, and makes
 * ordering between two coroutines genuinely nondeterministic. Injecting them means a test swaps
 * in `StandardTestDispatcher` and gets both back.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

/**
 * `Dispatchers.Main.immediate`. Use for anything that updates UI state and may already be on the
 * main thread — it dispatches without a post, so the update is visible in the same frame instead
 * of one frame later.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainImmediateDispatcher

/**
 * A scope that lives as long as the process rather than as long as a screen.
 *
 * For work that must finish even if the user leaves: flushing analytics, clearing a session
 * after logout, completing an upload. Anything launched in a `viewModelScope` for those is work
 * that gets cancelled halfway on a back press.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
