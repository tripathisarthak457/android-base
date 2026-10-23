plugins {
    id("com.example.standard.android.feature")
}

android {
    namespace = "com.example.standard.feature.sample"
}

dependencies {
    implementation(project(":data:sample"))
}
