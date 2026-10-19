plugins {
    id("__PKG__.android.data")
}

android {
    namespace = "__PKG__.data.__NAME__"
}

dependencies {
    implementation(project(":core:network"))

    testImplementation(libs.kotlinx.serialization.json)
}
