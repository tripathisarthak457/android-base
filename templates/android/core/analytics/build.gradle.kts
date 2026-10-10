plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.hilt")
}

android {
    namespace = "com.base.app.core.analytics"
}

dependencies {
    implementation(project(":core:common"))

    implementation(libs.androidx.metrics.performance)
    implementation(libs.androidx.lifecycle.process)

    // <opt:firebaseanalytics|crashlytics>
    implementation(platform(libs.firebase.bom))
    // </opt:firebaseanalytics|crashlytics>
    // <opt:firebaseanalytics>
    implementation(libs.firebase.analytics)
    // </opt:firebaseanalytics>
    // <opt:crashlytics>
    implementation(libs.firebase.crashlytics)
    // </opt:crashlytics>

    testImplementation(project(":core:testing"))
}
