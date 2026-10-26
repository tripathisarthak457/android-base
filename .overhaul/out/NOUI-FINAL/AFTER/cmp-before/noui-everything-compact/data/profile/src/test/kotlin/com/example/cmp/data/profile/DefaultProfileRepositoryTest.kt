package com.example.cmp.data.profile

import androidx.core.net.toUri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.cmp.core.common.result.AppError
import com.example.cmp.core.common.result.AppResult
import com.example.cmp.core.media.compress.ImageCompressor
import com.example.cmp.core.testing.testDispatchers
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class DefaultProfileRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val avatars by lazy { File(folder.root, "avatar") }

    private fun TestScope.repository(): DefaultProfileRepository {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(folder.root, "profile.preferences_pb")
        }
        val dispatchers = testDispatchers()
        return DefaultProfileRepository(
            store,
            ImageCompressor(RuntimeEnvironment.getApplication(), dispatchers),
            avatars,
            dispatchers,
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

    @Test
    fun `a photo that cannot be read is a storage failure and the current avatar stays`() = runTest {
        val repository = repository()
        val current = avatarCopy("avatar_1.jpg")

        val result = repository.setPhoto(File(folder.root, "gone.png").toUri().toString())

        assertTrue((result as AppResult.Failure).error is AppError.Storage)
        assertTrue(current.exists())
    }

    @Test
    fun `removing the photo deletes every copy`() = runTest {
        val repository = repository()
        avatarCopy("avatar_1.jpg")
        avatarCopy("avatar_2.jpg")

        repository.setPhoto(null)

        assertNull(repository.profile.first().photoUri)
        assertEquals(emptyList<File>(), avatars.listFiles()?.toList().orEmpty())
    }

    private fun avatarCopy(name: String): File =
        File(avatars.apply { mkdirs() }, name).apply { writeBytes(byteArrayOf(1)) }
}

// Robolectric refuses SDK 36 and above on a Java 17 toolchain.
private const val ROBOLECTRIC_SDK = 35
