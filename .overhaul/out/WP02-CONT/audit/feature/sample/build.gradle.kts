plugins {
    id("io.example.audit.android.feature")
}

android {
    namespace = "io.example.audit.feature.sample"
}

dependencies {
    implementation(project(":data:sample"))
}
