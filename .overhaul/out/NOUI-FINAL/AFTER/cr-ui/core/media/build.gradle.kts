plugins {
    id("com.example.finalafter.crui.android.library")
    id("com.example.finalafter.crui.android.hilt")
    id("com.example.finalafter.crui.android.compose")
}

android {
    namespace = "com.example.finalafter.crui.core.media"
}

dependencies {
    implementation(project(":core:common"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.exifinterface)
}
