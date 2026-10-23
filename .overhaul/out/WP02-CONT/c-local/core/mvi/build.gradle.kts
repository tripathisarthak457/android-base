plugins {
    id("io.example.fitclocal.android.library")
}

android {
    namespace = "io.example.fitclocal.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel)
}
