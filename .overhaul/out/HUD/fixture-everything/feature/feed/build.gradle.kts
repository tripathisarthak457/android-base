plugins {
    id("com.example.hud.fixture.android.feature")
}

android {
    namespace = "com.example.hud.fixture.feature.feed"
}

dependencies {
    implementation(project(":data:feed"))
}
