plugins {
    id("com.example.wp04.everything.android.library")
    id("com.example.wp04.everything.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.wp04.everything.core.datastore"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
