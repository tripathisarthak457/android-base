plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.base.app.core.datastore"
}

dependencies {
    implementation(project(":core:common"))

    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
