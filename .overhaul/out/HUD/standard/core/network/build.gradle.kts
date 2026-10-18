plugins {
    id("com.example.hud.standard.android.library")
    id("com.example.hud.standard.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.hud.standard.core.network"

    // The connectivity monitor is tested against Robolectric's ConnectivityManager.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:common"))
    // `api`: the session's tokens cross this module's API in SessionTokens.
    api(project(":core:datastore"))

    implementation(libs.androidx.core.ktx)

    // `api`: OkHttpClient is provided from here to whatever else talks HTTP, such as the image loader.
    api(libs.okhttp)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.logging)
    api(libs.kotlinx.serialization.json)

    testImplementation(project(":core:testing"))
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.robolectric)
}
