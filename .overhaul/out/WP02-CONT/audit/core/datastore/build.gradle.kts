plugins {
    id("io.example.audit.android.library")
    id("io.example.audit.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.example.audit.core.datastore"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
