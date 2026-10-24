plugins {
    id("com.example.deskstandard.android.feature")
}

android {
    namespace = "com.example.deskstandard.feature.sample"
}

dependencies {
    implementation(project(":data:sample"))
}
