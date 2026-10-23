package com.example.everything.core.video

import android.net.Uri
import androidx.media3.datasource.DataSpec
import com.example.everything.core.testing.compose.TEST_SDK
import com.example.everything.core.video.engine.videoNetwork
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Video goes through the app's client, built only when the first clip is fetched, with exact byte ranges. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class VideoNetworkTest {

    @Test
    fun `a range request keeps identity encoding and reaches the app's client, built on the first call`() {
        val seen = mutableListOf<Request>()
        var built = 0
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                seen += chain.request()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(206)
                    .message("Partial Content")
                    .header("Content-Range", "bytes 1024-2047/2048")
                    .body(ByteArray(1024).toResponseBody())
                    .build()
            }
            .build()
        val factory = videoNetwork {
            built++
            client
        }
        val source = factory.createDataSource()
        assertEquals(0, built)

        // Media3 sends a range only for a position past the start or a bounded length.
        source.open(DataSpec.Builder().setUri(Uri.parse("https://v.test/clip.mp4")).setPosition(1024).build())
        source.close()

        assertEquals(1, built)
        val request = seen.single()
        assertEquals("bytes=1024-", request.header("Range"))
        // Identity, so the client's own compression never touches the byte ranges a seek depends on.
        assertEquals(listOf("identity"), request.headers("Accept-Encoding"))
    }
}
