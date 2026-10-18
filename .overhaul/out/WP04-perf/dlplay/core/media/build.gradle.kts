plugins {
    id("com.example.wp04.dlplay.android.library")
    id("com.example.wp04.dlplay.android.hilt")
    id("com.example.wp04.dlplay.android.compose")
}

android {
    namespace = "com.example.wp04.dlplay.core.media"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.exifinterface)

    testImplementation(project(":core:testing"))
}
