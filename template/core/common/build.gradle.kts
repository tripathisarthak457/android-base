plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.hilt")
}

android {
    namespace = "com.base.app.core.common"
}

dependencies {
    api(project(":core:coroutines"))

    implementation(libs.androidx.core.ktx)
    // `api`, not `implementation`: MviViewModel.persistState takes a SavedStateHandle, so
    // every feature module that persists state needs the type on its own compile classpath.
    api(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
}
