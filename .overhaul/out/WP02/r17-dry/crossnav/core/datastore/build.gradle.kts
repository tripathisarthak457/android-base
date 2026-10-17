plugins {
    id("com.example.modulescompact.android.library")
    id("com.example.modulescompact.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.modulescompact.core.datastore"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
