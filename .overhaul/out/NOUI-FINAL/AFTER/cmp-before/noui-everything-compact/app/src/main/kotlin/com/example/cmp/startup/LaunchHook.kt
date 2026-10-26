package com.example.cmp.startup

import android.content.Intent
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/*
 * Seams only the dev flavour fills, for benchmarks and scripted measurements. In every other
 * flavour each set is empty, so a shipped build cannot be talked past its gates or retuned by an
 * intent.
 */

/** Runs with a fresh launch's intent before the entry gates read the settings and the session. */
fun interface LaunchHook {
    suspend fun beforeStart(intent: Intent)
}

@Module
@InstallIn(SingletonComponent::class)
interface LaunchHookModule {

    @Multibinds
    fun launchHooks(): Set<LaunchHook>
}
