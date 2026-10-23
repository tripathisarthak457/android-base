plugins {
    id("io.example.fitcnetdb.android.library")
    id("io.example.fitcnetdb.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.example.fitcnetdb.core.datastore"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
