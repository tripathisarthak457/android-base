plugins {
    id("com.base.app.android.feature")
}

android {
    namespace = "com.base.app.feature.profile"
}

dependencies {
    implementation(project(":data:profile"))
    // <opt:media>
    implementation(project(":core:media"))
    // </opt:media>
}
