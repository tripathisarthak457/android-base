plugins {
    id("com.example.finalafter.crui2.android.library")
    id("com.example.finalafter.crui2.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.finalafter.crui2.core.datastore"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
