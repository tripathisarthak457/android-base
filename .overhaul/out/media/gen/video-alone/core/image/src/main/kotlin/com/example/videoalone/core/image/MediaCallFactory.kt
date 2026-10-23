package com.example.videoalone.core.image

import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * The HTTP client images and video share when the app has none of its own: a project without the
 * networking module, or the catalog. The client is built on the first request, on the thread that
 * makes it: Coil's fetcher and Media3's loader call from their own threads, so this costs nothing at
 * start-up and nothing on the main thread.
 *
 * [userAgent] names the app to media hosts that ask automated clients to identify themselves.
 */
class MediaCallFactory internal constructor(
    private val userAgent: String?,
    private val build: (OkHttpClient.Builder) -> OkHttpClient,
) : Call.Factory {

    constructor(userAgent: String? = null) : this(userAgent, OkHttpClient.Builder::build)

    private val client by lazy {
        val builder = OkHttpClient.Builder()
        if (userAgent != null) {
            builder.addInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("User-Agent", userAgent).build()) }
        }
        build(builder)
    }

    override fun newCall(request: Request): Call = client.newCall(request)
}
