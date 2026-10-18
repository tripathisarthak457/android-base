plugins {
    id("com.example.hud.everything.android.library")
    id("com.example.hud.everything.android.hilt")
}

android {
    namespace = "com.example.hud.everything.core.flags"
}

dependencies {
    implementation(project(":core:common"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)

    testImplementation(project(":core:testing"))
}
