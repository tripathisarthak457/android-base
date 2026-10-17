plugins {
    id("com.example.modulescompact.android.data")
}

android {
    namespace = "com.example.modulescompact.data.wallet"
}

dependencies {
    implementation(project(":core:network"))

    testImplementation(libs.kotlinx.serialization.json)
}
