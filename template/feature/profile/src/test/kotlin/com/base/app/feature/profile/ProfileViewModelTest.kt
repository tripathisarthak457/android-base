package com.base.app.feature.profile

import com.base.app.core.testing.MainDispatcherRule
import com.base.app.data.profile.Profile
import com.base.app.data.profile.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
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
    fun `fields start from the stored profile`() = runTest {
        val viewModel = ProfileViewModel(FakeProfileRepository(Profile(name = "Ada", bio = "Engines")))
        advanceUntilIdle()

        assertEquals("Ada", viewModel.state.value.name)
        assertFalse(viewModel.state.value.isDirty)
    }

    @Test
    fun `a blank name is refused and nothing is written`() = runTest {
        val repository = FakeProfileRepository(Profile(name = "Ada"))
        val viewModel = ProfileViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(ProfileEvent.NameChanged("  "))
        viewModel.onEvent(ProfileEvent.Save)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.nameError)
        assertEquals("Ada", repository.state.value.name)
    }

    @Test
    fun `saving writes the trimmed values`() = runTest {
        val repository = FakeProfileRepository(Profile())
        val viewModel = ProfileViewModel(repository)
        advanceUntilIdle()

        viewModel.onEvent(ProfileEvent.NameChanged(" Grace "))
        viewModel.onEvent(ProfileEvent.Save)
        advanceUntilIdle()

        assertEquals("Grace", repository.state.value.name)
        assertFalse(viewModel.state.value.isDirty)
    }
}

private class FakeProfileRepository(initial: Profile) : ProfileRepository {
    val state = MutableStateFlow(initial)
    override val profile = state

    override suspend fun save(name: String, bio: String) {
        state.value = state.value.copy(name = name.trim(), bio = bio.trim())
    }

    override suspend fun setPhoto(uri: String?) {
        state.value = state.value.copy(photoUri = uri)
    }
}
