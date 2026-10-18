/*
 * The app's own database, as opposed to the network module's response cache.
 */
plugins {
    id("com.example.wp04.dlplay.android.library")
    id("com.example.wp04.dlplay.android.hilt")
    id("com.example.wp04.dlplay.android.room")
}

android {
    namespace = "com.example.wp04.dlplay.core.database"

    // Robolectric renders the migration test against a real SQLite, which needs the merged
    // resources and manifest.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:common"))

    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
}
