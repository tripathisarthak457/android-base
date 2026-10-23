plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.compose")
    // <opt:devtools>
    // The tooling seam's optional binding (`tooling/DevToolingModule.kt`).
    id("com.base.app.android.hilt")
    // </opt:devtools>
}

android {
    namespace = "com.base.app.core.ui"
}

dependencies {
    api(project(":core:mvi"))
    api(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    // <opt:appstatus>
    implementation(project(":core:network"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigationevent.compose)
    // </opt:appstatus>

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // <opt:browser>
    implementation(libs.androidx.browser)
    // </opt:browser>
    // <opt:paging>
    api(libs.androidx.paging.compose)
    // </opt:paging>
}
