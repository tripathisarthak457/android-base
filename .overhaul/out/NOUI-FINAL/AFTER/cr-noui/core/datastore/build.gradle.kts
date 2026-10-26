plugins {
    id("com.example.finalafter.crnoui.android.library")
    id("com.example.finalafter.crnoui.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.finalafter.crnoui.core.datastore"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
