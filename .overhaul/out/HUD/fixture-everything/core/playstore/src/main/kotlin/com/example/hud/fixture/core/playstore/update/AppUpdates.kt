package com.example.hud.fixture.core.playstore.update

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.hud.fixture.core.common.coroutines.AppDispatchers
import com.example.hud.fixture.core.common.coroutines.ApplicationScope
import com.example.hud.fixture.core.common.log.Log
import com.example.hud.fixture.core.playstore.di.PlayStoreData
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.ktx.AppUpdateResult
import com.google.android.play.core.ktx.requestUpdateFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Where a flexible update stands, as far as the person is concerned. */
internal sealed interface UpdateState {
    data object Idle : UpdateState

    /** A newer build is live and has not been turned down: offer it. */
    data class Offer(val available: AppUpdateResult.Available) : UpdateState

    /** Offered; Play's own sheet has the person's answer now. */
    data object Offered : UpdateState

    /** Downloaded and waiting for a restart, which only the person starts. */
    data class Ready(val downloaded: AppUpdateResult.Downloaded) : UpdateState
}

/**
 * Play's flexible in-app update, checked once per process rather than on every resume. A version
 * the person turned down is remembered and not offered again; a newer one is. A downloaded update
 * never restarts the app in front of the person: they are asked, and otherwise it is installed the
 * next time the app goes to the background, where Play installs it without a visible restart.
 */
@Singleton
class AppUpdates @Inject internal constructor(
    @ApplicationContext private val context: Context,
    @PlayStoreData private val store: DataStore<Preferences>,
    private val scope: ApplicationScope,
    private val dispatchers: AppDispatchers,
) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    internal val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var checked = false
    private var checking: Job? = null

    /** Starts the once-per-process check. Main thread. */
    internal fun check() {
        if (checked) return
        checked = true
        // On the main thread: the process lifecycle takes observers there only.
        checking = scope.launch(dispatchers.main) {
            val dismissed = store.data.first()[DISMISSED_VERSION]
            AppUpdateManagerFactory.create(context)
                .requestUpdateFlow()
                .catch { failure ->
                    // No Play Store on the device, or a build installed from somewhere else.
                    Log.w("Update check failed", failure, TAG)
                }
                .transformWhile { result ->
                    emit(result)
                    result !is AppUpdateResult.NotAvailable && result !is AppUpdateResult.Downloaded
                }
                .collect { result ->
                    when (result) {
                        is AppUpdateResult.Available -> if (shouldOffer(result.updateInfo.availableVersionCode(), dismissed)) {
                            if (_state.value == UpdateState.Idle) _state.value = UpdateState.Offer(result)
                        }
                        is AppUpdateResult.Downloaded -> {
                            _state.value = UpdateState.Ready(result)
                            ProcessLifecycleOwner.get().lifecycle.addObserver(InstallWhenHidden(result))
                        }
                        is AppUpdateResult.InProgress, AppUpdateResult.NotAvailable -> Unit
                    }
                }
        }
    }

    internal fun offered() {
        _state.value = UpdateState.Offered
    }

    /** The person closed Play's sheet without updating: this version is not offered again. */
    internal fun declined(versionCode: Int) {
        _state.value = UpdateState.Idle
        // Nothing more to hear from Play in this process: the download was never started.
        checking?.cancel()
        scope.launch { store.edit { it[DISMISSED_VERSION] = versionCode } }
    }

    internal fun restart(downloaded: AppUpdateResult.Downloaded) {
        scope.launch {
            try {
                downloaded.completeUpdate()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                Log.w("Could not install the downloaded update", failure, TAG)
            }
        }
    }

    private inner class InstallWhenHidden(private val downloaded: AppUpdateResult.Downloaded) : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            owner.lifecycle.removeObserver(this)
            restart(downloaded)
        }
    }

    private companion object {
        const val TAG = "AppUpdates"
        val DISMISSED_VERSION = intPreferencesKey("dismissed_update_version")
    }
}

/** A version is offered unless the person already turned down that one or a newer one. */
internal fun shouldOffer(availableVersionCode: Int, dismissedVersionCode: Int?): Boolean =
    dismissedVersionCode == null || availableVersionCode > dismissedVersionCode
