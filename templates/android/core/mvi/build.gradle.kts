plugins {
    id("com.base.app.android.library")
}

android {
    namespace = "com.base.app.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel)
}
