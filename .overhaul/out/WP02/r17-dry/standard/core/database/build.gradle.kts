/*
 * The app's own database, as opposed to the network module's response cache.
 */
plugins {
    id("com.example.standard.android.library")
    id("com.example.standard.android.hilt")
    id("com.example.standard.android.room")
}

android {
    namespace = "com.example.standard.core.database"

    // Robolectric renders the migration test against a real SQLite, which needs the merged
    // resources and manifest.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:common"))

    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
}
