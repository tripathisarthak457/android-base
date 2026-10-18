plugins {
    id("com.example.hud.fixture.android.data")
}

android {
    namespace = "com.example.hud.fixture.data.sample"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:database"))
}
