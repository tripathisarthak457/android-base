/*
 * The on-device inspector: the environment badge, and everything behind it.
 *
 * A module of its own so that it is a dependency a release build does not have. The badge, the
 * request log and the panel are all in here, and a project generated without this feature has no
 * code that could record a response body at all — which is the only version of "off in production"
 * that can be verified by looking.
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
