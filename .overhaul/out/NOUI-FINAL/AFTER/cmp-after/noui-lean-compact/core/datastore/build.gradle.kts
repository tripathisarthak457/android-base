plugins {
    id("com.example.cmp.android.library")
    id("com.example.cmp.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.cmp.core.datastore"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
