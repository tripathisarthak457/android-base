plugins {
    id("com.example.wp04.abstd.android.feature")
}

android {
    namespace = "com.example.wp04.abstd.feature.profile"
}

dependencies {
    implementation(project(":data:profile"))
    implementation(project(":core:media"))
}
