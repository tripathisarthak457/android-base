plugins {
    id("com.example.hud.everything.android.data")
}

android {
    namespace = "com.example.hud.everything.data.sample"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:database"))
}
