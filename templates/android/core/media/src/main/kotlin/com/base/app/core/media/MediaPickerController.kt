package com.base.app.core.media

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/** The picker actions a screen can trigger. */
@Stable
class MediaPickerController internal constructor(
    private val pickImageAction: () -> Unit,
    private val pickMultipleAction: () -> Unit,
    private val pickVideoAction: () -> Unit,
    private val pickDocumentAction: (Array<String>) -> Unit,
    private val takePhotoAction: () -> Unit,
    private val recordVideoAction: () -> Unit,
) {
    /**
     * The system photo picker. Needs no permission at all on API 33+, and on older releases runs
     * through a backport that also needs none.
     */
    fun pickImage() = pickImageAction()

    fun pickImages() = pickMultipleAction()

    fun pickVideo() = pickVideoAction()

    /** For a PDF, a spreadsheet, anything that is not media. */
    fun pickDocument(mimeTypes: Array<String> = arrayOf("application/pdf")) =
        pickDocumentAction(mimeTypes)

    /** Opens the camera app. The result lands at a file this controller created. */
    fun takePhoto() = takePhotoAction()

    /** Records a video, with the duration and quality limits applied *at capture*. */
    fun recordVideo() = recordVideoAction()
}

/** Wires up every picker a screen might need and reports results through one callback. */
@Composable
fun rememberMediaPicker(
    videoSettings: VideoCompression = VideoCompression.Standard,
    onResult: (List<Uri>) -> Unit,
): MediaPickerController {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { currentOnResult(listOf(it)) } }

    val pickMultiple = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_SELECTION),
    ) { uris -> if (uris.isNotEmpty()) currentOnResult(uris) }

    val pickDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            // Without this the URI is readable until the process dies and then is not, which
            // surfaces as an upload that works in testing and fails after a background kill.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            currentOnResult(listOf(it))
        }
    }

    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { saved ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (saved && uri != null) currentOnResult(listOf(uri))
    }

    val recordVideo = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo(),
    ) { saved ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (saved && uri != null) currentOnResult(listOf(uri))
    }

    return remember(context, videoSettings) {
        MediaPickerController(
            pickImageAction = {
                pickImage.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            pickMultipleAction = {
                pickMultiple.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            pickVideoAction = {
                pickImage.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                )
            },
            pickDocumentAction = { mimeTypes -> pickDocument.launch(mimeTypes) },
            takePhotoAction = {
                val uri = context.createCaptureUri("photo", "jpg")
                pendingCameraUri = uri
                takePhoto.launch(uri)
            },
            recordVideoAction = {
                val uri = context.createCaptureUri("video", "mp4")
                pendingCameraUri = uri
                recordVideo.launch(uri)
            },
        )
    }
}

/** A file in this app's cache, exposed through the FileProvider declared in this module. */
private fun Context.createCaptureUri(prefix: String, extension: String): Uri {
    val directory = File(cacheDir, CAPTURE_DIRECTORY).apply { mkdirs() }
    val file = File(directory, "${prefix}_${System.currentTimeMillis()}.$extension")
    return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}

/** The video-capture extras the platform understands. */
fun VideoCompression.captureExtras(): Map<String, Any> = buildMap {
    maxDurationSeconds?.let { put(MediaStore.EXTRA_DURATION_LIMIT, it) }
    // 0 is the low-quality/MMS profile, 1 is high. Anything below 720p wants the former.
    put(MediaStore.EXTRA_VIDEO_QUALITY, if (maxDimension >= HIGH_QUALITY_THRESHOLD) 1 else 0)
}

private const val CAPTURE_DIRECTORY = "captures"
private const val MAX_SELECTION = 10
private const val HIGH_QUALITY_THRESHOLD = 1280
