plugins {
    id("com.example.hud.standard.android.feature")
}

android {
    namespace = "com.example.hud.standard.feature.profile"
}

dependencies {
    implementation(project(":data:profile"))
}
