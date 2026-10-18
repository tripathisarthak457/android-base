package com.example.hud.fixture.core.media.picker

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/** What a screen can ask for. Each result arrives through the callback given to [rememberMediaPicker]. */
@Stable
class MediaPicker internal constructor(
    private val pickImageAction: () -> Unit,
    private val pickImagesAction: () -> Unit,
    private val takePhotoAction: () -> Unit,
) {
    /** The system photo picker: no permission needed on any supported version. */
    fun pickImage() = pickImageAction()

    /** The photo picker with several images allowed, up to ten. */
    fun pickImages() = pickImagesAction()

    /** The camera app, writing to a file this app owns. No permission needed: this app never opens the camera itself. */
    fun takePhoto() = takePhotoAction()
}

/**
 * Wires up the photo picker and the camera for one screen. The images are only readable while this
 * process lives; copy what should outlast it (`ImageCompressor` writes a copy the app owns).
 */
@Composable
fun rememberMediaPicker(onResult: (List<Uri>) -> Unit): MediaPicker {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    // Saved: the camera app often pushes this activity, or the whole process, out of memory, and
    // coming back the photo would be dropped for want of knowing where it went.
    var pendingCapture by rememberSaveable { mutableStateOf<Uri?>(null) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { currentOnResult(listOf(it)) }
    }
    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_SELECTION)) { uris ->
        if (uris.isNotEmpty()) currentOnResult(uris)
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingCapture
        pendingCapture = null
        if (saved && uri != null) currentOnResult(listOf(uri))
    }

    return remember(context) {
        val imagesOnly = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        MediaPicker(
            pickImageAction = { pickImage.launch(imagesOnly) },
            pickImagesAction = { pickImages.launch(imagesOnly) },
            takePhotoAction = {
                val uri = context.newCaptureUri()
                pendingCapture = uri
                takePhoto.launch(uri)
            },
        )
    }
}

/** A file under `cacheDir/captures/`, shared with the camera app through this module's FileProvider. */
private fun Context.newCaptureUri(): Uri {
    val directory = File(cacheDir, CAPTURE_DIRECTORY).apply { mkdirs() }
    val file = File(directory, "photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}

private const val CAPTURE_DIRECTORY = "captures"
private const val MAX_SELECTION = 10
