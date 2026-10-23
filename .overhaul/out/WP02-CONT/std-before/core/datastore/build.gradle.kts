plugins {
    id("com.example.standard.android.library")
    id("com.example.standard.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.standard.core.datastore"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
