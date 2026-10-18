plugins {
    id("com.example.hud.everything.android.library")
    id("com.example.hud.everything.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.hud.everything.core.datastore"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
