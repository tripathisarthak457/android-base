plugins {
    id("com.example.everything.android.feature")
}

android {
    namespace = "com.example.everything.feature.feed"
}

dependencies {
    implementation(project(":data:feed"))
}
