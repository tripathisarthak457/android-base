plugins {
    id("com.example.modulescompact.android.library")
}

android {
    namespace = "com.example.modulescompact.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel)
}
