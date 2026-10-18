plugins {
    id("com.example.wp04.everything.android.feature")
}

android {
    namespace = "com.example.wp04.everything.feature.profile"
}

dependencies {
    implementation(project(":data:profile"))
    implementation(project(":core:media"))
}
