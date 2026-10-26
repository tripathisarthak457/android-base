package com.example.cmp.startup

import android.content.Intent
import com.example.cmp.core.navigation.shell.ShellConfig
import com.example.cmp.core.navigation.shell.ShellState
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/*
 * Seams only the dev flavour fills, for benchmarks and tools/perf. In every other flavour each set
 * is empty, so a shipped build cannot be talked past its gates or retuned by an intent.
 */

/** Runs with a fresh launch's intent before the entry gates read the settings and the session. */
fun interface LaunchHook {
    suspend fun beforeStart(intent: Intent)
}

/** Adjusts the shell's configuration, after the launch hooks have run. */
fun interface ShellConfigOverride {
    fun adjust(config: ShellConfig): ShellConfig
}

/** Watches the signed-in shell's state for as long as it is on screen. */
fun interface ShellObserver {
    suspend fun observe(state: ShellState)
}

@Module
@InstallIn(SingletonComponent::class)
interface LaunchHookModule {

    @Multibinds
    fun launchHooks(): Set<LaunchHook>

    @Multibinds
    fun shellConfigOverrides(): Set<ShellConfigOverride>

    @Multibinds
    fun shellObservers(): Set<ShellObserver>
}
