plugins {
    id("com.example.hud.fixture.android.feature")
}

android {
    namespace = "com.example.hud.fixture.feature.profile"
}

dependencies {
    implementation(project(":data:profile"))
    implementation(project(":core:media"))
}
