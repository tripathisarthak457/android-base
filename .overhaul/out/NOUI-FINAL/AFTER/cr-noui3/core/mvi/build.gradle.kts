plugins {
    id("com.example.finalafter.crnoui3.android.library")
}

android {
    namespace = "com.example.finalafter.crnoui3.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel)
}
