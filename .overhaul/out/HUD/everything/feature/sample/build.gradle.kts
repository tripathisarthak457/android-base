plugins {
    id("com.example.hud.everything.android.feature")
}

android {
    namespace = "com.example.hud.everything.feature.sample"
}

dependencies {
    implementation(project(":data:sample"))
}
