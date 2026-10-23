plugins {
    id("io.example.fitcnetdb.android.library")
}

android {
    namespace = "io.example.fitcnetdb.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel)
}
