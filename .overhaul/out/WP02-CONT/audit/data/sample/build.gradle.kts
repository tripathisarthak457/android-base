plugins {
    id("io.example.audit.android.data")
}

android {
    namespace = "io.example.audit.data.sample"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:database"))
}
