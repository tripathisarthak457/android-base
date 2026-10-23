plugins {
    id("io.example.fitclocal.android.library")
    id("io.example.fitclocal.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.example.fitclocal.core.datastore"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
