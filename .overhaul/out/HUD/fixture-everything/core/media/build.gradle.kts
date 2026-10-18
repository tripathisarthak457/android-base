plugins {
    id("com.example.hud.fixture.android.library")
    id("com.example.hud.fixture.android.hilt")
    id("com.example.hud.fixture.android.compose")
}

android {
    namespace = "com.example.hud.fixture.core.media"
}

dependencies {
    implementation(project(":core:common"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.exifinterface)
}
