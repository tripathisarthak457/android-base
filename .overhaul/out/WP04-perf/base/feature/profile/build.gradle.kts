plugins {
    id("com.example.wp04.base.android.feature")
}

android {
    namespace = "com.example.wp04.base.feature.profile"
}

dependencies {
    implementation(project(":data:profile"))
    implementation(project(":core:media"))
}
