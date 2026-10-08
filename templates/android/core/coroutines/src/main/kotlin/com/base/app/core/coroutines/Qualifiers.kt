package com.base.app.core.coroutines

import javax.inject.Qualifier

/** Dispatchers are injected, never referenced as `Dispatchers.IO` inside a class. */
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
 * main thread — it dispatches without a post, so the update is visible in the same frame instead of
 * one frame later.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainImmediateDispatcher

/** A scope that lives as long as the process rather than as long as a screen. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
