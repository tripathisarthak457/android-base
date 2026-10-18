plugins {
    id("com.example.hud.lean.android.feature")
}

android {
    namespace = "com.example.hud.lean.feature.sample"
}

dependencies {
    implementation(project(":data:sample"))
}
