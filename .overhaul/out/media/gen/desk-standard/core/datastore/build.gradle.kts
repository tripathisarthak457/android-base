plugins {
    id("com.example.deskstandard.android.library")
    id("com.example.deskstandard.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.deskstandard.core.datastore"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
