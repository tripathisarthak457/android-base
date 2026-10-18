plugins {
    id("com.example.wp04.abstd.android.library")
    id("com.example.wp04.abstd.android.hilt")
    id("com.example.wp04.abstd.android.compose")
}

android {
    namespace = "com.example.wp04.abstd.core.media"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.exifinterface)

    testImplementation(project(":core:testing"))
}
