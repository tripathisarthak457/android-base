plugins {
    id("io.example.fit.android.data")
}

android {
    namespace = "io.example.fit.data.abcde"
}

dependencies {
    implementation(project(":core:network"))

    testImplementation(libs.kotlinx.serialization.json)
}
