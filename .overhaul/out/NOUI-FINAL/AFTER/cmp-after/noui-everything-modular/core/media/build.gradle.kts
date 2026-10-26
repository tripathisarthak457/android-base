plugins {
    id("com.example.cmp.android.library")
    id("com.example.cmp.android.hilt")
    id("com.example.cmp.android.compose")
}

android {
    namespace = "com.example.cmp.core.media"
}

dependencies {
    implementation(project(":core:common"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.exifinterface)
}
