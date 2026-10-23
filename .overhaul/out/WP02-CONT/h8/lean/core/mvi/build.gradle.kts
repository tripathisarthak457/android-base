plugins {
    id("com.example.lean.android.library")
}

android {
    namespace = "com.example.lean.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel)
}
