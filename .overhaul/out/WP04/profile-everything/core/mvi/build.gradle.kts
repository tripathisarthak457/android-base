plugins {
    id("com.example.wp04.everything.android.library")
}

android {
    namespace = "com.example.wp04.everything.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel.ktx)

    testImplementation(project(":core:testing"))
}
