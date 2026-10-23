package io.example.fitcnetdb.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.example.fitcnetdb.core.common.coroutines.AppDispatchers
import io.example.fitcnetdb.core.common.coroutines.ApplicationScope
import io.example.fitcnetdb.core.common.log.Log
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Classes get their dispatchers injected as [AppDispatchers] instead of naming `Dispatchers.*`, so a
 * test can run all of them on its own scheduler.
 */
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
     * background work until the process restarts. What a child throws is logged, and reported when
     * a crash reporter is bound, instead of killing the app for work nobody was waiting on.
     */
    @Provides
    @Singleton
    fun provideApplicationScope(
        dispatchers: AppDispatchers,
    ): ApplicationScope = ApplicationScope(
        CoroutineScope(
            SupervisorJob() + dispatchers.default + CoroutineExceptionHandler { _, throwable ->
                Log.e("Uncaught in the application scope", throwable)
            },
        ),
    )
}
