plugins {
    id("com.example.none.android.library")
}

android {
    namespace = "com.example.none.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel)
}
