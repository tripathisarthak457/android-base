plugins {
    id("com.example.hud.standard.android.data")
}

android {
    namespace = "com.example.hud.standard.data.profile"
}

dependencies {
    implementation(project(":core:datastore"))
}
