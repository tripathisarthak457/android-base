plugins {
    id("com.example.everything.android.library")
    id("com.example.everything.android.hilt")
    id("com.example.everything.android.compose")
}

android {
    namespace = "com.example.everything.core.media"
}

dependencies {
    implementation(project(":core:common"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.exifinterface)
}
