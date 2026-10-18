plugins {
    id("com.example.hud.standard.android.feature")
}

android {
    namespace = "com.example.hud.standard.feature.sample"
}

dependencies {
    implementation(project(":data:sample"))
}
