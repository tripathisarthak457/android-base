plugins {
    id("com.example.wp04.everything.android.library")
    id("com.example.wp04.everything.android.hilt")
    id("com.example.wp04.everything.android.compose")
}

android {
    namespace = "com.example.wp04.everything.core.media"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.exifinterface)

    testImplementation(project(":core:testing"))
}
