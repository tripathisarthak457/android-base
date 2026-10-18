plugins {
    id("com.example.hud.fixture.android.feature")
}

android {
    namespace = "com.example.hud.fixture.feature.search"
}

dependencies {
    implementation(project(":data:search"))
}
