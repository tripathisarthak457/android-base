plugins {
    id("com.example.standardmodular.android.library")
}

android {
    namespace = "com.example.standardmodular.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel)
}
