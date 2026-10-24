/*
 * Play's in-app update and review prompts.
 */
plugins {
    id("com.example.everything.android.library")
    id("com.example.everything.android.compose")
    id("com.example.everything.android.hilt")
}

android {
    namespace = "com.example.everything.core.playstore"

    // The prompts' test renders under Robolectric, which needs the merged manifest and resources.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))

    implementation(libs.play.app.update)
    implementation(libs.play.app.review)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(project(":core:testing"))
    testImplementation(project(":core:testing:compose"))
}
