plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.compose")
    id("com.base.app.android.hilt")
}

android {
    namespace = "com.base.app.core.ui"
}

dependencies {
    api(project(":core:common"))
    api(project(":core:designsystem"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // <opt:coil>
    implementation(libs.coil.compose)
    // </opt:coil>
}
