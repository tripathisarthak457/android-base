plugins {
    id("com.example.none.android.library")
    id("com.example.none.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.none.core.datastore"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
