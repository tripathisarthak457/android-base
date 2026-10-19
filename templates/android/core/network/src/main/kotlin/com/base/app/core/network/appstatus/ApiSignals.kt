package com.base.app.core.network.appstatus

import com.base.app.core.common.result.AppError
import com.base.app.core.network.client.NetworkConfig
import com.base.app.core.network.model.NetworkResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** A state of the whole service rather than of the screen that asked. */
enum class AppStatus {
    Available,

    /** The server no longer accepts this version. Sticky until the app is updated. */
    UpdateRequired,

    /** The server is down for maintenance. Clears when the person retries and a request gets through. */
    Maintenance,
}

/**
 * Which refusals mean [AppStatus.UpdateRequired] and [AppStatus.Maintenance]. The defaults follow
 * HTTP; a backend that says either with a code in its error body lists the code here.
 */
data class AppStatusRules(
    val updateRequiredStatuses: Set<Int> = setOf(426),
    val updateRequiredCodes: Set<String> = emptySet(),
    /** Empty: any 503 is maintenance. Otherwise only a 503 whose code is listed, since a proxy answers 503 too. */
    val maintenanceCodes: Set<String> = emptySet(),
    /**
     * A response header holding the oldest version code the server still supports, checked on every
     * answer, so an update can be required before this version is refused outright. Null checks none.
     */
    val minimumVersionHeader: String? = null,
)

/**
 * Raised by the network layer on any call and shown by the shell, so no screen handles a forced
 * update or a maintenance window by itself.
 */
@Singleton
class ApiSignals @Inject constructor(
    private val rules: AppStatusRules,
    private val config: NetworkConfig,
) {
    private val _status = MutableStateFlow(AppStatus.Available)
    val status: StateFlow<AppStatus> = _status.asStateFlow()

    internal fun onAnswer(response: NetworkResponse) {
        val header = rules.minimumVersionHeader ?: return
        val minimum = response.header(header)?.toLongOrNull() ?: return
        if (minimum > config.appVersionCode) _status.value = AppStatus.UpdateRequired
    }

    internal fun onRefusal(error: AppError.Http) {
        val signal = when {
            error.status in rules.updateRequiredStatuses || error.code in rules.updateRequiredCodes ->
                AppStatus.UpdateRequired

            error.status == SERVICE_UNAVAILABLE &&
                (rules.maintenanceCodes.isEmpty() || error.code in rules.maintenanceCodes) -> AppStatus.Maintenance

            else -> return
        }
        _status.update { current -> if (current == AppStatus.UpdateRequired) current else signal }
    }

    /** The person asked to try again: the next request decides whether maintenance is over. */
    fun retry() {
        _status.update { current -> if (current == AppStatus.Maintenance) AppStatus.Available else current }
    }

    private companion object {
        const val SERVICE_UNAVAILABLE = 503
    }
}
