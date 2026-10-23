plugins {
    id("com.base.app.android.data")
}

android {
    namespace = "com.base.app.data.profile"
    // <opt:media>

    // The avatar test runs the compressor under Robolectric, which needs the merged manifest.
    testOptions.unitTests.isIncludeAndroidResources = true
    // </opt:media>
}

dependencies {
    implementation(project(":core:datastore"))
    // <opt:media>
    implementation(project(":core:media"))
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.robolectric)
    // </opt:media>
}
