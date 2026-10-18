plugins {
    id("com.example.hud.everything.android.feature")
}

android {
    namespace = "com.example.hud.everything.feature.profile"
}

dependencies {
    implementation(project(":data:profile"))
    implementation(project(":core:media"))
}
