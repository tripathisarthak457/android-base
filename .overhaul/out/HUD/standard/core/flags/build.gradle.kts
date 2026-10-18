plugins {
    id("com.example.hud.standard.android.library")
    id("com.example.hud.standard.android.hilt")
}

android {
    namespace = "com.example.hud.standard.core.flags"
}

dependencies {
    implementation(project(":core:common"))

    testImplementation(project(":core:testing"))
}
