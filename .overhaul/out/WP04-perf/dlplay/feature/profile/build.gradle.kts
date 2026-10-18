plugins {
    id("com.example.wp04.dlplay.android.feature")
}

android {
    namespace = "com.example.wp04.dlplay.feature.profile"
}

dependencies {
    implementation(project(":data:profile"))
    implementation(project(":core:media"))
}
