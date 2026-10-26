package com.example.finalafter.crui2.core.applock

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.finalafter.crui2.core.common.coroutines.ApplicationScope
import com.example.finalafter.crui2.core.datastore.settings.AppSettingsStore
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppLockTest {

    @get:Rule
    val folder = TemporaryFolder()

    private var now = 1_000_000L
    private var deviceSecure = true
    private val owner = object : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this)
    }

    @Test
    fun `a cold start with the lock on is locked from the first frame`() = runTest {
        val lock = lock(enabled = true)

        assertTrue(lock.isLockedNow())
        assertTrue(lock.locked.first())
    }

    @Test
    fun `Home on the unlock screen and straight back is still locked`() = runTest {
        val lock = lock(enabled = true)

        leaveFor(lock, millis = 2_000)

        assertTrue(lock.isLockedNow())
    }

    @Test
    fun `passing the challenge in the system's credential screen does not lock again on return`() = runTest {
        val lock = lock(enabled = true)

        lock.onStop(owner)
        now += AppLock.GRACE_MILLIS + 1
        lock.unlocked()
        lock.onStart(owner)

        assertFalse(lock.isLockedNow())
    }

    @Test
    fun `a success reported after the return does not lock again either`() = runTest {
        val lock = lock(enabled = true)

        lock.onStop(owner)
        now += AppLock.GRACE_MILLIS + 1
        lock.onStart(owner)
        lock.unlocked()

        assertFalse(lock.isLockedNow())
    }

    @Test
    fun `a short trip away after unlocking does not ask again, a long one does`() = runTest {
        val lock = lock(enabled = true)
        lock.unlocked()

        leaveFor(lock, millis = AppLock.GRACE_MILLIS)
        assertFalse(lock.isLockedNow())

        leaveFor(lock, millis = AppLock.GRACE_MILLIS + 1)
        assertTrue(lock.isLockedNow())
    }

    @Test
    fun `turning the lock on does not lock the person who just did it`() = runTest {
        val settings = settings()
        val lock = lock(settings)
        settings.settings.first()
        runCurrent()
        assertFalse(lock.isLockedNow())

        settings.setAppLockEnabled(true)
        settings.settings.first { it.appLockEnabled }
        runCurrent()
        assertFalse(lock.isLockedNow())

        leaveFor(lock, millis = AppLock.GRACE_MILLIS + 1)
        assertTrue(lock.isLockedNow())
    }

    @Test
    fun `a phone with no screen lock is never locked`() = runTest {
        deviceSecure = false
        val lock = lock(enabled = true)

        leaveFor(lock, millis = AppLock.GRACE_MILLIS + 1)

        assertFalse(lock.isLockedNow())
    }

    private fun leaveFor(lock: AppLock, millis: Long) {
        lock.onStop(owner)
        now += millis
        lock.onStart(owner)
    }

    private suspend fun TestScope.lock(enabled: Boolean): AppLock {
        val settings = settings()
        settings.setAppLockEnabled(enabled)
        settings.settings.first { it.appLockEnabled == enabled }
        return lock(settings)
    }

    private fun TestScope.lock(settings: AppSettingsStore): AppLock {
        val lock = AppLock(settings, { deviceSecure }, { now }, ApplicationScope(backgroundScope))
        runCurrent()
        return lock
    }

    private fun TestScope.settings(): AppSettingsStore {
        val scope: CoroutineScope = backgroundScope
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "settings.preferences_pb") }
        return AppSettingsStore(dataStore, ApplicationScope(scope)).also { runCurrent() }
    }
}
