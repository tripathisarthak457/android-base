package com.base.app.di

import com.base.app.core.common.coroutines.AppDispatchers
import com.base.app.core.common.coroutines.ApplicationScope
import com.base.app.core.common.log.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/** The one place `Dispatchers.*` is named; every class gets them injected as [AppDispatchers]. */
@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

    @Provides
    @Singleton
    fun provideDispatchers(): AppDispatchers = AppDispatchers(
        io = Dispatchers.IO,
        default = Dispatchers.Default,
        main = Dispatchers.Main.immediate,
    )

    /**
     * A [SupervisorJob], so one failed child does not cancel the scope and stop every other piece of
     * background work until the process restarts. What a child throws is logged, and reaches the
     * crash reporter through it, instead of killing the app for work nobody was waiting on.
     */
    @Provides
    @Singleton
    fun provideApplicationScope(dispatchers: AppDispatchers): ApplicationScope = ApplicationScope(
        CoroutineScope(
            SupervisorJob() + dispatchers.default + CoroutineExceptionHandler { _, throwable ->
                Log.e("Uncaught in the application scope", throwable)
            },
        ),
    )
}
