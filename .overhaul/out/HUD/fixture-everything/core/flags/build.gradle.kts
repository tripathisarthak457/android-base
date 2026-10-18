plugins {
    id("com.example.hud.fixture.android.library")
    id("com.example.hud.fixture.android.hilt")
}

android {
    namespace = "com.example.hud.fixture.core.flags"
}

dependencies {
    implementation(project(":core:common"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)

    testImplementation(project(":core:testing"))
}
