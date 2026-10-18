plugins {
    id("com.example.hud.lean.android.data")
}

android {
    namespace = "com.example.hud.lean.data.sample"
}

dependencies {
    implementation(project(":core:network"))
}
