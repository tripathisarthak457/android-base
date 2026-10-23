plugins {
    id("com.example.videoalone.android.library")
}

android {
    namespace = "com.example.videoalone.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel)
}
