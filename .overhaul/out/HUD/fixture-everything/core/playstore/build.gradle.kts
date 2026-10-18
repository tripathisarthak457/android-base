/*
 * Play's in-app update and review prompts.
 */
plugins {
    id("com.example.hud.fixture.android.library")
    id("com.example.hud.fixture.android.compose")
    id("com.example.hud.fixture.android.hilt")
}

android {
    namespace = "com.example.hud.fixture.core.playstore"
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
}
