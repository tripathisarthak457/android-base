plugins {
    id("com.example.everything.android.feature")
}

android {
    namespace = "com.example.everything.feature.search"
}

dependencies {
    implementation(project(":data:search"))
}
