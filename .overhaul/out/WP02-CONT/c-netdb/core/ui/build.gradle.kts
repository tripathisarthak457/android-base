plugins {
    id("io.example.fitcnetdb.android.library")
    id("io.example.fitcnetdb.android.compose")
}

android {
    namespace = "io.example.fitcnetdb.core.ui"
}

dependencies {
    api(project(":core:mvi"))
    api(project(":core:designsystem"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
