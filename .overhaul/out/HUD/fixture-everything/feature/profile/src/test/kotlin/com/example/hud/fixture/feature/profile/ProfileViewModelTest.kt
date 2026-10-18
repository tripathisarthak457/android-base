package com.example.hud.fixture.feature.profile

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import com.example.hud.fixture.core.mvi.MessageKind
import com.example.hud.fixture.core.testing.MainDispatcherRule
import com.example.hud.fixture.data.profile.api.model.Profile
import com.example.hud.fixture.data.profile.api.ProfileRepository
import com.example.hud.fixture.core.common.result.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `fields start from the stored profile`() = runTest {
        val viewModel = appeared(FakeProfileRepository(Profile(name = "Ada", bio = "Engines")))

        assertEquals("Ada", viewModel.name.text.toString())
        assertTrue(viewModel.state.value.loaded)
        assertFalse(viewModel.state.value.isDirty)
    }

    @Test
    fun `text restored after process death is not overwritten by the stored value`() = runTest {
        // The saved state of a process that died mid-edit: the fields were filled before it died.
        val viewModel = ProfileViewModel(FakeProfileRepository(Profile(name = "Ada")), SavedStateHandle(mapOf("filled" to true)))
        viewModel.name.type("Ada Lovelace")

        viewModel.onEvent(ProfileEvent.Appeared)
        advanceUntilIdle()

        assertEquals("Ada Lovelace", viewModel.name.text.toString())
    }

    @Test
    fun `a blank name is refused and nothing is written`() = runTest {
        val repository = FakeProfileRepository(Profile(name = "Ada"))
        val viewModel = appeared(repository)

        viewModel.name.type("  ")
        viewModel.onEvent(ProfileEvent.Save)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.nameError)
        assertEquals("Ada", repository.state.value.name)
    }

    @Test
    fun `saving writes the trimmed values`() = runTest {
        val repository = FakeProfileRepository(Profile())
        val viewModel = appeared(repository)

        viewModel.name.type(" Grace ")
        viewModel.onEvent(ProfileEvent.Save)
        advanceUntilIdle()

        assertEquals("Grace", repository.state.value.name)
        assertFalse(viewModel.state.value.isDirty)
    }

    @Test
    fun `a save the store refuses is reported`() = runTest {
        val repository = FakeProfileRepository(Profile(name = "Ada"), refuseWrites = true)
        val viewModel = appeared(repository)

        viewModel.name.type("Grace")
        viewModel.onEvent(ProfileEvent.Save)
        advanceUntilIdle()

        assertEquals(MessageKind.Error, viewModel.messages.first().kind)
        assertEquals("Ada", repository.state.value.name)
    }

    private fun TestScope.appeared(repository: ProfileRepository): ProfileViewModel =
        ProfileViewModel(repository, SavedStateHandle()).also {
            it.state.launchIn(backgroundScope)
            it.onEvent(ProfileEvent.Appeared)
            advanceUntilIdle()
        }
}

/** What a keyboard does to the field, plus the frame tick that tells snapshot observers about it. */
private fun TextFieldState.type(text: String) {
    edit { replace(0, length, text) }
    Snapshot.sendApplyNotifications()
}

private class FakeProfileRepository(
    initial: Profile,
    private val refuseWrites: Boolean = false,
) : ProfileRepository {
    val state = MutableStateFlow(initial)
    override val profile = state

    override suspend fun save(name: String, bio: String) {
        if (refuseWrites) throw IOException("No space left on device")
        state.value = state.value.copy(name = name.trim(), bio = bio.trim())
    }

    override suspend fun setPhoto(picked: String?): AppResult<Unit> {
        state.value = state.value.copy(photoUri = picked)
        return AppResult.Success(Unit)
    }
}
