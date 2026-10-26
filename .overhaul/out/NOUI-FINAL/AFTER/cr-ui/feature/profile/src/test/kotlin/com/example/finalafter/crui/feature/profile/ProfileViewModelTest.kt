package com.example.finalafter.crui.feature.profile

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import com.example.finalafter.crui.core.mvi.MessageKind
import com.example.finalafter.crui.core.testing.MainDispatcherRule
import com.example.finalafter.crui.data.profile.api.FakeProfileRepository
import com.example.finalafter.crui.data.profile.api.ProfileRepository
import com.example.finalafter.crui.data.profile.api.model.Profile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `the fields wait for the screen before they read the store`() = runTest {
        val viewModel = ProfileViewModel(FakeProfileRepository(Profile(name = "Ada")), SavedStateHandle())

        advanceUntilIdle()

        assertEquals("", viewModel.name.text.toString())
        assertFalse(viewModel.state.value.loaded)
    }

    @Test
    fun `fields start from the stored profile`() = runTest {
        val viewModel = shown(FakeProfileRepository(Profile(name = "Ada", bio = "Engines")))

        assertEquals("Ada", viewModel.name.text.toString())
        assertTrue(viewModel.state.value.loaded)
        assertFalse(viewModel.state.value.isDirty)
    }

    @Test
    fun `text restored after process death is not overwritten by the stored value`() = runTest {
        // The saved state of a process that died mid-edit: the fields were filled before it died.
        val viewModel = ProfileViewModel(FakeProfileRepository(Profile(name = "Ada")), SavedStateHandle(mapOf("filled" to true)))
        viewModel.name.type("Ada Lovelace")

        viewModel.state.launchIn(backgroundScope)
        runCurrent()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.loaded)
        assertEquals("Ada Lovelace", viewModel.name.text.toString())
    }

    @Test
    fun `a blank name is refused and nothing is written`() = runTest {
        val repository = FakeProfileRepository(Profile(name = "Ada"))
        val viewModel = shown(repository)

        viewModel.name.type("  ")
        viewModel.onEvent(ProfileEvent.Save)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.nameError)
        assertEquals("Ada", repository.stored.name)
    }

    @Test
    fun `saving writes the trimmed values`() = runTest {
        val repository = FakeProfileRepository(Profile())
        val viewModel = shown(repository)

        viewModel.name.type(" Grace ")
        viewModel.onEvent(ProfileEvent.Save)
        advanceUntilIdle()

        assertEquals("Grace", repository.stored.name)
        assertFalse(viewModel.state.value.isDirty)
    }

    @Test
    fun `a save the store refuses is reported`() = runTest {
        val repository = FakeProfileRepository(Profile(name = "Ada"), refuseWrites = true)
        val viewModel = shown(repository)

        viewModel.name.type("Grace")
        viewModel.onEvent(ProfileEvent.Save)
        advanceUntilIdle()

        assertEquals(MessageKind.Error, viewModel.messages.first().kind)
        assertEquals("Ada", repository.stored.name)
    }

    /** A profile screen on show: its route collects the state once it is started. */
    private fun TestScope.shown(repository: ProfileRepository): ProfileViewModel =
        ProfileViewModel(repository, SavedStateHandle()).also {
            it.state.launchIn(backgroundScope)
            runCurrent()
            advanceUntilIdle()
        }
}

/** What a keyboard does to the field, plus the frame tick that tells snapshot observers about it. */
private fun TextFieldState.type(text: String) {
    edit { replace(0, length, text) }
    Snapshot.sendApplyNotifications()
}
