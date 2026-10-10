plugins {
    id("com.base.app.android.data")
}

android {
    namespace = "com.base.app.data.profile"
}

dependencies {
    implementation(project(":core:datastore"))
    // <opt:media>
    implementation(project(":core:media"))
    implementation(libs.androidx.core.ktx)
    // </opt:media>
}
