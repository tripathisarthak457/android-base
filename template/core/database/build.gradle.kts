/*
 * The app's own database, as opposed to the network module's response cache.
 *
 * Separate because they answer to different owners: the cache is an implementation detail of how
 * requests are made and is safe to delete at any moment, while this holds what the user typed and
 * must survive an upgrade. One database for both would make the second promise about the first.
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
