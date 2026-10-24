plugins {
    id("com.example.everything.android.feature")
}

android {
    namespace = "com.example.everything.feature.profile"
}

dependencies {
    implementation(project(":data:profile"))
    implementation(project(":core:media"))
    implementation(project(":core:image"))
}
