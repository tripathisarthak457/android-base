plugins {
    id("com.example.modulesmodular.android.library")
}

android {
    namespace = "com.example.modulesmodular.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel)
}
