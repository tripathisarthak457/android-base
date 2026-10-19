plugins {
    id("com.example.cicheck.android.library")
    id("com.example.cicheck.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.cicheck.core.datastore"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
