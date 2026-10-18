plugins {
    id("com.example.hud.lean.android.library")
}

android {
    namespace = "com.example.hud.lean.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel.ktx)

    testImplementation(project(":core:testing"))
}
