/*
 * The app's own data: one Room database, a package per domain.
 */
plugins {
    id("com.example.finalafter.crui2.android.library")
    id("com.example.finalafter.crui2.android.hilt")
    id("com.example.finalafter.crui2.android.room")
}

android {
    namespace = "com.example.finalafter.crui2.core.database"

    // The DAO tests run against the platform's SQLite under Robolectric, which needs the merged
    // manifest.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:common"))

    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
}
