plugins {
    id("com.example.hud.lean.android.library")
    id("com.example.hud.lean.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.hud.lean.core.datastore"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
