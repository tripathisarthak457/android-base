plugins {
    id("io.example.audit.android.library")
    id("io.example.audit.android.compose")
}

android {
    namespace = "io.example.audit.core.ui"
}

dependencies {
    api(project(":core:mvi"))
    api(project(":core:designsystem"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
