plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.base.app.core.network"

    // The connectivity monitor is tested against Robolectric's ConnectivityManager.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:common"))
    // `api`: the session's tokens cross this module's API in SessionTokens.
    api(project(":core:datastore"))
    // <opt:offlinequeue>
    implementation(project(":core:database"))
    implementation(libs.androidx.work.runtime)
    // </opt:offlinequeue>

    implementation(libs.androidx.core.ktx)
    // <opt:websocket>
    implementation(libs.androidx.lifecycle.process)
    // </opt:websocket>

    // `api`: OkHttpClient is provided from here to whatever else talks HTTP, such as the image loader.
    api(libs.okhttp)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.logging)
    // <opt:websocket>
    implementation(libs.ktor.client.websockets)
    // </opt:websocket>
    api(libs.kotlinx.serialization.json)

    testImplementation(project(":core:testing"))
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.robolectric)
}
