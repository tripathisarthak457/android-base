plugins {
    id("com.example.hud.everything.android.library")
    id("com.example.hud.everything.android.hilt")
    id("com.example.hud.everything.android.compose")
}

android {
    namespace = "com.example.hud.everything.core.media"
}

dependencies {
    implementation(project(":core:common"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.exifinterface)
}
