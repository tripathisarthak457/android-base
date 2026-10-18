plugins {
    id("com.example.hud.standard.android.data")
}

android {
    namespace = "com.example.hud.standard.data.sample"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:database"))
}
