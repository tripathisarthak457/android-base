plugins {
    id("com.example.modulescompact.android.data")
}

android {
    namespace = "com.example.modulescompact.data.orders"
}

dependencies {
    api(project(":data:orders:api"))
    implementation(project(":core:network"))

    testImplementation(libs.kotlinx.serialization.json)
}
