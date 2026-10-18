plugins {
    id("com.example.wp04.base.android.library")
    id("com.example.wp04.base.android.hilt")
    id("com.example.wp04.base.android.compose")
}

android {
    namespace = "com.example.wp04.base.core.media"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:coroutines"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.exifinterface)

    testImplementation(project(":core:testing"))
}
