plugins {
    id("com.example.everything.android.data")
}

android {
    namespace = "com.example.everything.data.profile"

    // The avatar test runs the compressor under Robolectric, which needs the merged manifest.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:datastore"))
    implementation(project(":core:media"))
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.robolectric)
}
