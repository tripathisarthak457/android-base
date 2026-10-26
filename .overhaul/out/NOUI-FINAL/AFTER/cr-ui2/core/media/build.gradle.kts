plugins {
    id("com.example.finalafter.crui2.android.library")
    id("com.example.finalafter.crui2.android.hilt")
    id("com.example.finalafter.crui2.android.compose")
}

android {
    namespace = "com.example.finalafter.crui2.core.media"
}

dependencies {
    implementation(project(":core:common"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.exifinterface)
}
