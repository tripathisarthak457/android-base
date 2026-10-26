package com.base.app.data.profile

// <opt:media>
import androidx.core.net.toUri
// </opt:media>
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
// <opt:media>
import com.base.app.core.common.result.AppError
import com.base.app.core.common.result.AppResult
import com.base.app.core.media.compress.ImageCompressor
import com.base.app.core.testing.testDispatchers
// </opt:media>
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
// <opt:media>
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
// </opt:media>
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
// <opt:media>
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
// </opt:media>

// <opt:media>
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
// </opt:media>
class DefaultProfileRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    // <opt:media>
    private val avatars by lazy { File(folder.root, "avatar") }

    // </opt:media>
    private fun TestScope.repository(): DefaultProfileRepository {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(folder.root, "profile.preferences_pb")
        }
        // <opt:media>
        val dispatchers = testDispatchers()
        // </opt:media>
        return DefaultProfileRepository(
            store,
            // <opt:media>
            ImageCompressor(RuntimeEnvironment.getApplication(), dispatchers),
            avatars,
            dispatchers,
            // </opt:media>
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
    // <opt:media>

    @Test
    fun `a photo that cannot be read is a storage failure and the current avatar stays`() =
        runTest {
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
    // </opt:media>
}
// <opt:media>

// Robolectric refuses SDK 36 and above on a Java 17 toolchain.
private const val ROBOLECTRIC_SDK = 35
// </opt:media>
