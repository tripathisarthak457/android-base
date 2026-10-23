plugins {
    id("io.example.fitcnet.android.data")
}

android {
    namespace = "io.example.fitcnet.data.abcdefghijklmnopqrs"
}

dependencies {
    implementation(project(":core:network"))

    testImplementation(libs.kotlinx.serialization.json)
}
