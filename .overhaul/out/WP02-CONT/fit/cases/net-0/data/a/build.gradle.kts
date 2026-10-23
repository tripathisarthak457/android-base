plugins {
    id("io.example.fit.android.data")
}

android {
    namespace = "io.example.fit.data.a"
}

dependencies {
    implementation(project(":core:network"))

    testImplementation(libs.kotlinx.serialization.json)
}
