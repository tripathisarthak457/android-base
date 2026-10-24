/*
 * The app's own data: one Room database, a package per domain.
 */
plugins {
    id("io.example.audit.android.library")
    id("io.example.audit.android.hilt")
    id("io.example.audit.android.room")
}

android {
    namespace = "io.example.audit.core.database"

    // The DAO tests run against the platform's SQLite under Robolectric, which needs the merged
    // manifest.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:common"))

    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
}
