plugins {
    id("com.example.standardmodular.android.library")
    id("com.example.standardmodular.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.standardmodular.core.datastore"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
