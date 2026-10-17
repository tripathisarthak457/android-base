plugins {
    id("com.example.modulescompact.android.data")
}

android {
    namespace = "com.example.modulescompact.data.wallet"
}

dependencies {
    api(project(":data:wallet:api"))
    implementation(project(":core:network"))

    testImplementation(libs.kotlinx.serialization.json)
}
