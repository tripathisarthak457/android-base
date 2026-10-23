plugins {
    id("io.example.fitcnet.android.library")
}

android {
    namespace = "io.example.fitcnet.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel)
}
