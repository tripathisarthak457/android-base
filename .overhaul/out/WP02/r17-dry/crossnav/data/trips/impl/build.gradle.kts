plugins {
    id("com.example.modulescompact.android.data")
}

android {
    namespace = "com.example.modulescompact.data.trips"
}

dependencies {
    api(project(":data:trips:api"))
    implementation(project(":core:network"))

    testImplementation(libs.kotlinx.serialization.json)
}
