plugins {
    id("com.example.everything.android.feature")
}

android {
    namespace = "com.example.everything.feature.sample"
}

dependencies {
    implementation(project(":data:sample"))
}
