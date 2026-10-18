plugins {
    id("com.example.hud.everything.android.feature")
}

android {
    namespace = "com.example.hud.everything.feature.search"
}

dependencies {
    implementation(project(":data:search"))
}
