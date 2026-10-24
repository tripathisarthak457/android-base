plugins {
    id("io.example.audit.android.data")
}

android {
    namespace = "io.example.audit.data.abcdefghijklmnopqrstuvwxyzabcd"
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:network"))

    testImplementation(libs.kotlinx.serialization.json)
}
