plugins {
    id("com.example.modulesmodular.android.data")
}

android {
    namespace = "com.example.modulesmodular.data.orders"
}

dependencies {
    api(project(":data:orders:api"))
    implementation(project(":core:network"))

    testImplementation(libs.kotlinx.serialization.json)
}
