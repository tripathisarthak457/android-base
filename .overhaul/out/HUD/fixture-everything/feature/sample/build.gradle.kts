plugins {
    id("com.example.hud.fixture.android.feature")
}

android {
    namespace = "com.example.hud.fixture.feature.sample"
}

dependencies {
    implementation(project(":data:sample"))
}
