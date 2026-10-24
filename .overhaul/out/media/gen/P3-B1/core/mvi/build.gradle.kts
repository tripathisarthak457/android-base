plugins {
    id("com.example.everything.android.library")
}

android {
    namespace = "com.example.everything.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel)
}
