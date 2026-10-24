plugins {
    id("io.example.audit.android.library")
}

android {
    namespace = "io.example.audit.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel)
}
