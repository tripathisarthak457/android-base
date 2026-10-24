plugins {
    id("com.example.everything.android.library")
    id("com.example.everything.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.everything.core.datastore"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
