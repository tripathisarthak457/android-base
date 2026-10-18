plugins {
    id("com.example.hud.fixture.android.library")
}

android {
    namespace = "com.example.hud.fixture.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel.ktx)

    testImplementation(project(":core:testing"))
}
