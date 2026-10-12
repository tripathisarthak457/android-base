package com.base.app.shared.di

import androidx.datastore.core.DataStore
import androidx.lifecycle.SavedStateHandle
import com.base.app.shared.AppEnvironment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

/**
 * Every type a definition's constructor asks for is defined somewhere in the app's module list, so
 * a binding a feature forgot fails here, on the JVM, rather than when its screen first opens on a
 * device.
 */
class KoinModulesTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun appModules_defineEveryConstructorDependency() {
        val environment = AppEnvironment(
            apiBaseUrl = "https://api.example.com/",
            name = "dev",
            isDebug = true,
            versionName = "1.0.0",
        )

        module { includes(appModules(environment)) }.verify(extraTypes = HandedIn)
    }
}

/**
 * Types the graph is handed rather than asked for: what a definition builds inline and passes
 * straight to a constructor, the collections the registries are built from, and the
 * `SavedStateHandle` the ViewModel factory supplies. `Boolean` is here because the values built
 * from [AppEnvironment] carry flags, and Koin's own allow-list of plain types stops at numbers and
 * strings.
 */
private val HandedIn = listOf(
    Boolean::class,
    CoroutineDispatcher::class,
    CoroutineScope::class,
    DataStore::class,
    List::class,
    Set::class,
    Map::class,
    SavedStateHandle::class,
)
