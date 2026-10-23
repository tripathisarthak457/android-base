plugins {
    id("io.example.fitcnetdb.android.data")
}

android {
    namespace = "io.example.fitcnetdb.data.abcdefghi"
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:network"))

    testImplementation(libs.kotlinx.serialization.json)
}
