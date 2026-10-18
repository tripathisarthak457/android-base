plugins {
    id("com.example.hud.everything.android.feature")
}

android {
    namespace = "com.example.hud.everything.feature.feed"
}

dependencies {
    implementation(project(":data:feed"))
}
