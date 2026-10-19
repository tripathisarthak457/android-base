plugins {
    id("com.example.cicheck.android.feature")
}

android {
    namespace = "com.example.cicheck.feature.sample"
}

dependencies {
    implementation(project(":data:sample"))
}
