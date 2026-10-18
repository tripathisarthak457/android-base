plugins {
    id("com.example.hud.standard.android.library")
}

android {
    namespace = "com.example.hud.standard.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel.ktx)

    testImplementation(project(":core:testing"))
}
