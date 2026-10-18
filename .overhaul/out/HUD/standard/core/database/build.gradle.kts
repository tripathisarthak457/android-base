/*
 * The app's own data: one Room database, a package per domain.
 */
plugins {
    id("com.example.hud.standard.android.library")
    id("com.example.hud.standard.android.hilt")
    id("com.example.hud.standard.android.room")
}

android {
    namespace = "com.example.hud.standard.core.database"

    // The DAO tests run against the platform's SQLite under Robolectric, which needs the merged
    // manifest.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:common"))

    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
}
