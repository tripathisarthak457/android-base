plugins {
    id("com.example.wp04.base.android.library")
    id("com.example.wp04.base.android.hilt")
}

android {
    namespace = "com.example.wp04.base.core.common"
}

dependencies {
    api(project(":core:coroutines"))

    implementation(libs.androidx.core.ktx)
    // `api`, not `implementation`: MviViewModel.persistState takes a SavedStateHandle, so
    // every feature module that persists state needs the type on its own compile classpath.
    api(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
}
