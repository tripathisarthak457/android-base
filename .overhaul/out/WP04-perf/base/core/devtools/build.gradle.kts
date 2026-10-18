/*
 * The on-device inspector: the environment badge, and everything behind it.
 */
plugins {
    id("com.example.wp04.base.android.library")
    id("com.example.wp04.base.android.compose")
    id("com.example.wp04.base.android.hilt")
}

android {
    namespace = "com.example.wp04.base.core.devtools"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))

    implementation(project(":core:flags"))
}
