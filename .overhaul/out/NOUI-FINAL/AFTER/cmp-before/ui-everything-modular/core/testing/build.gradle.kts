plugins {
    id("com.example.cmp.android.library")
}

android {
    namespace = "com.example.cmp.core.testing"
}

dependencies {
    api(project(":core:common"))

    // `api`, not `implementation`: this module exists to be a test module's single dependency,
    // so everything it offers has to reach that module's compile classpath.
    api(libs.junit)
    api(libs.turbine)
    api(libs.kotlinx.coroutines.test)
}
