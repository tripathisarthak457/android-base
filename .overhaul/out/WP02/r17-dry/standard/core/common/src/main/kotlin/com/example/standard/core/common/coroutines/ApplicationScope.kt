package com.example.standard.core.common.coroutines

import kotlinx.coroutines.CoroutineScope

/**
 * A scope that lives as long as the process rather than as long as a screen: for work that must
 * finish even when the screen that started it goes away. Its own type, so it is injected by type
 * and never mistaken for some other `CoroutineScope`.
 */
class ApplicationScope(scope: CoroutineScope) : CoroutineScope by scope
