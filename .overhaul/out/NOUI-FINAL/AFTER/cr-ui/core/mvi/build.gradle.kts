plugins {
    id("com.example.finalafter.crui.android.library")
}

android {
    namespace = "com.example.finalafter.crui.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel)
}
