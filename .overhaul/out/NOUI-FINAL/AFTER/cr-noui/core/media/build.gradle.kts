plugins {
    id("com.example.finalafter.crnoui.android.library")
    id("com.example.finalafter.crnoui.android.hilt")
    id("com.example.finalafter.crnoui.android.compose")
}

android {
    namespace = "com.example.finalafter.crnoui.core.media"
}

dependencies {
    implementation(project(":core:common"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.exifinterface)
}
