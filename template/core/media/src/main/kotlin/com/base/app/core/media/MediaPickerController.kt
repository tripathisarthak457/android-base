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

/**
 * The picker actions a screen can trigger.
 *
 * A single object rather than four separate `rememberLauncherForActivityResult` calls, so a screen
 * that offers "take a photo or choose one" is two lines rather than two launchers, two temp-file
 * dances and a `when` over which one came back.
 */
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
     * The system photo picker.
     *
     * Needs no permission at all on API 33+, and on older releases runs through a backport that
     * also needs none. An app that requests `READ_MEDIA_IMAGES` to show a picker is asking for
     * access to *every* photo on the device in order to receive one — which is what the
     * permission dialog tells the user, and why they decline.
     */
    fun pickImage() = pickImageAction()

    fun pickImages() = pickMultipleAction()

    fun pickVideo() = pickVideoAction()

    /** For a PDF, a spreadsheet, anything that is not media. */
    fun pickDocument(mimeTypes: Array<String> = arrayOf("application/pdf")) =
        pickDocumentAction(mimeTypes)

    /** Opens the camera app. The result lands at a file this controller created. */
    fun takePhoto() = takePhotoAction()

    /**
     * Records a video, with the duration and quality limits applied *at capture*.
     *
     * This is the cheap half of video compression: a camera app told to record 60 seconds at
     * standard quality produces a small file directly, with no re-encoding. See [VideoTranscoder]
     * for why the expensive half is a seam rather than an implementation.
     */
    fun recordVideo() = recordVideoAction()
}

/**
 * Wires up every picker a screen might need and reports results through one callback.
 *
 * ## The camera path needs a file up front
 *
 * `TakePicture` writes to a `Uri` you supply — it does not hand one back. That URI must come from
 * a `FileProvider`, because passing a `file://` URI to another app has thrown `FileUriExposedException`
 * since Android 7. The provider is declared in this module's manifest, so nothing is needed in
 * the app's.
 *
 * ## Cancellation is silent
 *
 * Every launcher reports null or false when the user backs out, and none of them call
 * [onResult] — a picker that fires an "operation failed" message when someone changes their mind
 * is worse than one that says nothing.
 */
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

/**
 * A file in this app's cache, exposed through the FileProvider declared in this module.
 *
 * The camera app writes here directly, so there is no copy step and no second full-size bitmap in
 * memory.
 */
private fun Context.createCaptureUri(prefix: String, extension: String): Uri {
    val directory = File(cacheDir, CAPTURE_DIRECTORY).apply { mkdirs() }
    val file = File(directory, "${prefix}_${System.currentTimeMillis()}.$extension")
    return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}

/**
 * The video-capture extras the platform understands.
 *
 * Exposed so a caller building its own intent applies the same limits. `EXTRA_DURATION_LIMIT` and
 * `EXTRA_VIDEO_QUALITY` are honoured by the stock camera app and by most OEM ones; a camera app
 * that ignores them still produces a usable file, so this is a hint rather than a guarantee.
 */
fun VideoCompression.captureExtras(): Map<String, Any> = buildMap {
    maxDurationSeconds?.let { put(MediaStore.EXTRA_DURATION_LIMIT, it) }
    // 0 is the low-quality/MMS profile, 1 is high. Anything below 720p wants the former.
    put(MediaStore.EXTRA_VIDEO_QUALITY, if (maxDimension >= HIGH_QUALITY_THRESHOLD) 1 else 0)
}

private const val CAPTURE_DIRECTORY = "captures"
private const val MAX_SELECTION = 10
private const val HIGH_QUALITY_THRESHOLD = 1280
