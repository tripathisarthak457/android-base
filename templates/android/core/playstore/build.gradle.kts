/*
 * Play's in-app update and review prompts.
 */
plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.compose")
    id("com.base.app.android.hilt")
}

android {
    namespace = "com.base.app.core.playstore"

    // The prompts' test renders under Robolectric, which needs the merged manifest and resources.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    // <opt:ui_components>
    implementation(project(":core:designsystem"))
    // </opt:ui_components>
    // <opt-else:ui_components>
    //~ // "Update ready" goes to whatever shows the app's messages: LocalMessagePresenter.
    //~ implementation(project(":core:ui"))
    // </opt-else:ui_components>

    implementation(libs.play.app.update)
    implementation(libs.play.app.review)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(project(":core:testing"))
    testImplementation(testFixtures(project(":core:datastore")))
    testImplementation(project(":core:testing:compose"))
}
