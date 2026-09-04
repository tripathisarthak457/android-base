plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.hilt")
}

android {
    namespace = "com.base.app.core.analytics"
}

dependencies {
    implementation(project(":core:common"))

    // <opt:analytics-firebase>
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    // </opt:analytics-firebase>
    // <opt:crashlytics>
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    // </opt:crashlytics>

    testImplementation(project(":core:testing"))
}
