/*
 * The on-device inspector: the environment badge, and everything behind it.
 */
plugins {
    id("com.example.standard.android.library")
    id("com.example.standard.android.compose")
    id("com.example.standard.android.hilt")
}

android {
    namespace = "com.example.standard.core.devtools"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))

    implementation(project(":core:flags"))
}
