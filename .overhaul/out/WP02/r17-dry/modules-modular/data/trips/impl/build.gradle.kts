plugins {
    id("com.example.modulesmodular.android.data")
}

android {
    namespace = "com.example.modulesmodular.data.trips"
}

dependencies {
    api(project(":data:trips:api"))
    implementation(project(":core:network"))

    testImplementation(libs.kotlinx.serialization.json)
}
