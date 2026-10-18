package com.example.hud.standard.data.profile

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DefaultProfileRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.repository(): DefaultProfileRepository {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(folder.root, "profile.preferences_pb")
        }
        return DefaultProfileRepository(
            store,
        )
    }

    @Test
    fun `what the person typed is kept without the spaces around it`() = runTest {
        val repository = repository()

        repository.save(name = "  Ada Lovelace ", bio = "\nAnalyst\n")

        val profile = repository.profile.first()
        assertEquals("Ada Lovelace", profile.name)
        assertEquals("Analyst", profile.bio)
    }
}
