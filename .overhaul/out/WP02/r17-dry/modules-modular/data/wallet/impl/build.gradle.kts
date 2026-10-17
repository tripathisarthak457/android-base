plugins {
    id("com.example.modulesmodular.android.data")
}

android {
    namespace = "com.example.modulesmodular.data.wallet"
}

dependencies {
    api(project(":data:wallet:api"))
    implementation(project(":core:network"))

    testImplementation(libs.kotlinx.serialization.json)
}
