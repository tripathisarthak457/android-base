plugins {
    id("com.example.finalafter.crnoui.android.library")
}

android {
    namespace = "com.example.finalafter.crnoui.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel)
}
