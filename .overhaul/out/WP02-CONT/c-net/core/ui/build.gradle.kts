plugins {
    id("io.example.fitcnet.android.library")
    id("io.example.fitcnet.android.compose")
}

android {
    namespace = "io.example.fitcnet.core.ui"
}

dependencies {
    api(project(":core:mvi"))
    api(project(":core:designsystem"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
