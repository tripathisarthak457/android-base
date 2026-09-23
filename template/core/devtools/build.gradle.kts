/*
 * The on-device inspector: the environment badge, and everything behind it.
 */
plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.compose")
    id("com.base.app.android.hilt")
}

android {
    namespace = "com.base.app.core.devtools"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))

    // <opt:flags>
    implementation(project(":core:flags"))
    // </opt:flags>
}
