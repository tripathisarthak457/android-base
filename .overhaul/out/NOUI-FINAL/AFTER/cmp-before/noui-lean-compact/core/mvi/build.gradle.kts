plugins {
    id("com.example.cmp.android.library")
}

android {
    namespace = "com.example.cmp.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel)
}
