plugins {
    id("com.example.finalafter.crui2.android.library")
}

android {
    namespace = "com.example.finalafter.crui2.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel)
}
