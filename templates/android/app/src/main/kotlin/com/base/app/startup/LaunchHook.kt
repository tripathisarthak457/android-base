package com.base.app.startup

import android.content.Intent
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
// <opt:ui_components>
import com.base.app.core.navigation.shell.ShellConfig
import com.base.app.core.navigation.shell.ShellState
// </opt:ui_components>

// <opt:ui_components>
/*
 * Seams only the dev flavour fills, for benchmarks and tools/perf. In every other flavour each set
 * is empty, so a shipped build cannot be talked past its gates or retuned by an intent.
 */
// </opt:ui_components>
// <opt-else:ui_components>
//~ /*
//~  * Seams only the dev flavour fills, for benchmarks and scripted measurements. In every other
//~  * flavour each set is empty, so a shipped build cannot be talked past its gates or retuned by an
//~  * intent.
//~  */
// </opt-else:ui_components>

/** Runs with a fresh launch's intent before the entry gates read the settings and the session. */
fun interface LaunchHook {
    suspend fun beforeStart(intent: Intent)
}
// <opt:ui_components>

/** Adjusts the shell's configuration, after the launch hooks have run. */
fun interface ShellConfigOverride {
    fun adjust(config: ShellConfig): ShellConfig
}

/** Watches the signed-in shell's state for as long as it is on screen. */
fun interface ShellObserver {
    suspend fun observe(state: ShellState)
}
// </opt:ui_components>

@Module
@InstallIn(SingletonComponent::class)
interface LaunchHookModule {

    @Multibinds
    fun launchHooks(): Set<LaunchHook>
    // <opt:ui_components>

    @Multibinds
    fun shellConfigOverrides(): Set<ShellConfigOverride>

    @Multibinds
    fun shellObservers(): Set<ShellObserver>
    // </opt:ui_components>
}
