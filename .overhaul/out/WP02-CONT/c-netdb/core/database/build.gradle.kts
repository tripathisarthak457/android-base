/*
 * The app's own data: one Room database, a package per domain.
 */
plugins {
    id("io.example.fitcnetdb.android.library")
    id("io.example.fitcnetdb.android.hilt")
    id("io.example.fitcnetdb.android.room")
}

android {
    namespace = "io.example.fitcnetdb.core.database"

    // The DAO tests run against the platform's SQLite under Robolectric, which needs the merged
    // manifest.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:common"))

    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
}
