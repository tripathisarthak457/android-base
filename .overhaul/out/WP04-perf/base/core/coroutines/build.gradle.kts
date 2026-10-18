plugins {
    id("com.example.wp04.base.android.library")
    id("com.example.wp04.base.android.hilt")
}

android {
    namespace = "com.example.wp04.base.core.coroutines"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}
