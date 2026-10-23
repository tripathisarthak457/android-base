plugins {
    id("com.example.videoalone.android.library")
    id("com.example.videoalone.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.videoalone.core.datastore"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
