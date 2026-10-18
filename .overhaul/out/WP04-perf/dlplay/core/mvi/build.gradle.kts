plugins {
    id("com.example.wp04.dlplay.android.library")
}

android {
    namespace = "com.example.wp04.dlplay.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel.ktx)

    testImplementation(project(":core:testing"))
}
