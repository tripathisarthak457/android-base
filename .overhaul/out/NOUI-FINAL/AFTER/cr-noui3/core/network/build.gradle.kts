plugins {
    id("com.example.finalafter.crnoui3.android.library")
    id("com.example.finalafter.crnoui3.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.finalafter.crnoui3.core.network"

    // The connectivity monitor is tested against Robolectric's ConnectivityManager.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:common"))
    // `api`: the session's tokens cross this module's API in SessionTokens.
    api(project(":core:datastore"))
    implementation(project(":core:database"))
    implementation(libs.androidx.work.runtime)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.process)

    // `api`: OkHttpClient is provided from here to whatever else talks HTTP, such as the image loader.
    api(libs.okhttp)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.websockets)
    api(libs.kotlinx.serialization.json)

    testImplementation(project(":core:testing"))
    testImplementation(testFixtures(project(":core:datastore")))
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.robolectric)
}
