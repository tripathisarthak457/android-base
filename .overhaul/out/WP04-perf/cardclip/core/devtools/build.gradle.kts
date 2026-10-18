/*
 * The on-device inspector: the environment badge, and everything behind it.
 */
plugins {
    id("com.example.wp04.everything.android.library")
    id("com.example.wp04.everything.android.compose")
    id("com.example.wp04.everything.android.hilt")
}

android {
    namespace = "com.example.wp04.everything.core.devtools"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))

    implementation(project(":core:flags"))
}
