plugins {
    id("io.example.fit.android.data")
}

android {
    namespace = "io.example.fit.data.abcdefghijklmnop"
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:network"))

    testImplementation(libs.kotlinx.serialization.json)
}
