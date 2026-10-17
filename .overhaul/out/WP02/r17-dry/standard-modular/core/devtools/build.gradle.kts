/*
 * The on-device inspector: the environment badge, and everything behind it.
 */
plugins {
    id("com.example.standardmodular.android.library")
    id("com.example.standardmodular.android.compose")
    id("com.example.standardmodular.android.hilt")
}

android {
    namespace = "com.example.standardmodular.core.devtools"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))

    implementation(project(":core:flags"))
}
