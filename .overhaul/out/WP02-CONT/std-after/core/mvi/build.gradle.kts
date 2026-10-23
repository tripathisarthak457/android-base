plugins {
    id("com.example.standard.android.library")
}

android {
    namespace = "com.example.standard.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel)
}
