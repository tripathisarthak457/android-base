plugins {
    id("io.example.fitcnet.android.library")
    id("io.example.fitcnet.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.example.fitcnet.core.datastore"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
