package com.base.app.core.devtools.screen

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The destination on top of the app's back stack, by name, as the app's root reports it. The
 * inspector shows it, and tooling that records the app labels what it records with it.
 */
@Singleton
class CurrentScreen @Inject constructor() {

    private val _name = MutableStateFlow<String?>(null)

    /** Null until the root reports its first destination. */
    val name: StateFlow<String?> = _name.asStateFlow()

    fun report(name: String) {
        _name.value = name
    }
}
