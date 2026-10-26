/*
 * The app's own data: one Room database, a package per domain.
 */
plugins {
    id("com.example.cmp.android.library")
    id("com.example.cmp.android.hilt")
    id("com.example.cmp.android.room")
}

android {
    namespace = "com.example.cmp.core.database"

    // The DAO tests run against the platform's SQLite under Robolectric, which needs the merged
    // manifest.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:common"))

    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
}
