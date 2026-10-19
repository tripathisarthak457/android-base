plugins {
    id("com.example.cicheck.android.library")
}

android {
    namespace = "com.example.cicheck.core.mvi"
}

dependencies {
    api(project(":core:common"))
    api(libs.androidx.lifecycle.viewmodel.ktx)

    testImplementation(project(":core:testing"))
}
