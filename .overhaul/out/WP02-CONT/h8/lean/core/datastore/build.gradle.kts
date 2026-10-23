plugins {
    id("com.example.lean.android.library")
    id("com.example.lean.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.lean.core.datastore"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
