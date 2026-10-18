plugins {
    id("com.example.wp04.everything.android.feature")
}

android {
    namespace = "com.example.wp04.everything.feature.search"
}

dependencies {
    implementation(project(":data:search"))
}
