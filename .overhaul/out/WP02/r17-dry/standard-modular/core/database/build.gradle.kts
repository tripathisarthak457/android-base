/*
 * The app's own database, as opposed to the network module's response cache.
 */
plugins {
    id("com.example.standardmodular.android.library")
    id("com.example.standardmodular.android.hilt")
    id("com.example.standardmodular.android.room")
}

android {
    namespace = "com.example.standardmodular.core.database"

    // Robolectric renders the migration test against a real SQLite, which needs the merged
    // resources and manifest.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:common"))

    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
}
