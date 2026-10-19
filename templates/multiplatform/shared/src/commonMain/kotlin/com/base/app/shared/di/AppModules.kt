package com.base.app.shared.di

import com.base.app.core.common.coroutines.AppDispatchers
import com.base.app.core.common.coroutines.ApplicationScope
import com.base.app.core.common.log.Log
import com.base.app.core.datastore.di.dataStoreModule
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.NavGraphEntry
import com.base.app.core.navigation.NavKeySerialization
import com.base.app.core.navigation.NavRegistry
import com.base.app.shared.AppEnvironment
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.module.Module
import org.koin.dsl.module
// <opt:network>
import com.base.app.core.network.client.NetworkConfig
import com.base.app.core.network.di.networkModule
// </opt:network>
// <opt:database>
import com.base.app.core.database.di.databaseModule
// </opt:database>
// <opt:sample>
import com.base.app.data.sample.di.sampleDataModule
import com.base.app.feature.sample.navigation.sampleModule
// </opt:sample>
// <opt:settings>
import com.base.app.feature.settings.api.SettingsAppInfo
import com.base.app.feature.settings.navigation.settingsModule
// </opt:settings>
// <opt:onboarding>
import com.base.app.feature.onboarding.navigation.onboardingModule
// </opt:onboarding>
// <generated:koin-imports>

/** Every Koin module the app is built from, in one list: the graph `KoinModulesTest` verifies. */
fun appModules(environment: AppEnvironment): List<Module> = listOf(
    appModule(environment),
    dataStoreModule,
    // <opt:network>
    networkModule,
    // </opt:network>
    // <opt:database>
    databaseModule,
    // </opt:database>
    // <opt:sample>
    sampleDataModule,
    sampleModule,
    // </opt:sample>
    // <opt:settings>
    settingsModule,
    // </opt:settings>
    // <opt:onboarding>
    onboardingModule,
    // </opt:onboarding>
    // <generated:koin-modules>
)

/** What only the app can supply: the environment, the dispatchers, and navigation for the window. */
private fun appModule(environment: AppEnvironment) = module {
    single { environment }
    single { AppDispatchers(io = Dispatchers.IO, default = Dispatchers.Default, main = Dispatchers.Main.immediate) }
    single {
        val failures = CoroutineExceptionHandler { _, error -> Log.e("Uncaught in the application scope", error) }
        ApplicationScope(CoroutineScope(SupervisorJob() + Dispatchers.Default + failures))
    }

    single { AppNavigator() }
    // Every feature contributes its graph and its keys under its own qualifier; these collect them.
    single { NavRegistry(getAll<NavGraphEntry>().toSet()) }
    single { NavKeySerialization(getAll<SerializersModule>()) }

    // <opt:network>
    single { NetworkConfig(baseUrl = environment.apiBaseUrl, isDebug = environment.isDebug) }
    // </opt:network>
    // <opt:settings>
    single { SettingsAppInfo(versionName = environment.versionName) }
    // </opt:settings>
}
