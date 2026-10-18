plugins {
    id("com.example.hud.everything.android.library")
}

android {
    namespace = "com.example.hud.everything.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel.ktx)

    testImplementation(project(":core:testing"))
}
