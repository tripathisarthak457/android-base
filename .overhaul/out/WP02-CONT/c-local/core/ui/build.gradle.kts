plugins {
    id("io.example.fitclocal.android.library")
    id("io.example.fitclocal.android.compose")
}

android {
    namespace = "io.example.fitclocal.core.ui"
}

dependencies {
    api(project(":core:mvi"))
    api(project(":core:designsystem"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
