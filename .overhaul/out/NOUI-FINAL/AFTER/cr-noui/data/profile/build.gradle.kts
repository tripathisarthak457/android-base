plugins {
    id("com.example.finalafter.crnoui.android.data")
}

android {
    namespace = "com.example.finalafter.crnoui.data.profile"

    // The avatar test runs the compressor under Robolectric, which needs the merged manifest.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:datastore"))
    implementation(project(":core:media"))
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.robolectric)
}
