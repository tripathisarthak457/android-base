plugins {
    id("io.example.fitcnetdb.android.data")
}

android {
    namespace = "io.example.fitcnetdb.data.abcdefghijklmnopqrstuvwxyzabcd"
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:network"))

    testImplementation(libs.kotlinx.serialization.json)
}
