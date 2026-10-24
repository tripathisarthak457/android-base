plugins {
    id("com.example.deskstandard.android.library")
}

android {
    namespace = "com.example.deskstandard.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel)
}
