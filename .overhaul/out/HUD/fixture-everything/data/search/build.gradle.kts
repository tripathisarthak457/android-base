plugins {
    id("com.example.hud.fixture.android.data")
}

android {
    namespace = "com.example.hud.fixture.data.search"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))
}
