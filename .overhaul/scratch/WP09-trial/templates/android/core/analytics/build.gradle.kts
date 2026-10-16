plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.hilt")
}

android {
    namespace = "com.base.app.core.analytics"
}

dependencies {
    implementation(project(":core:common"))

    // <opt:firebase>
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    // </opt:firebase>

    testImplementation(project(":core:testing"))
}
