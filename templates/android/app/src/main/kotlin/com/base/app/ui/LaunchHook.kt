package com.base.app.ui

import android.content.Intent
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/**
 * Runs with a fresh launch's intent before the entry gates read the settings and the session. Only
 * the dev flavour contributes hooks (the launch options benchmarks and tools/perf use); in every
 * other flavour the set is empty, so a shipped build cannot be talked past its gates by an intent.
 */
fun interface LaunchHook {
    suspend fun beforeStart(intent: Intent)
}

@Module
@InstallIn(SingletonComponent::class)
interface LaunchHookModule {

    @Multibinds
    fun launchHooks(): Set<LaunchHook>
}
