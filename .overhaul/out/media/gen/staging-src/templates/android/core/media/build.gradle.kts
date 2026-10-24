plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.hilt")
    id("com.base.app.android.compose")
}

android {
    namespace = "com.base.app.core.media"
}

dependencies {
    implementation(project(":core:common"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.exifinterface)
}
