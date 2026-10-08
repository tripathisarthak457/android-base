/*
 * The app's own database, as opposed to the network module's response cache.
 */
plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.hilt")
    id("com.base.app.android.room")
}

android {
    namespace = "com.base.app.core.database"

    // Robolectric renders the migration test against a real SQLite, which needs the merged
    // resources and manifest.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:common"))

    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
}
