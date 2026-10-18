plugins {
    id("com.example.wp04.base.android.library")
    id("com.example.wp04.base.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.wp04.base.core.datastore"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:coroutines"))

    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
}
