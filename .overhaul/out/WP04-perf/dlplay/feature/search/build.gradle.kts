plugins {
    id("com.example.wp04.dlplay.android.feature")
}

android {
    namespace = "com.example.wp04.dlplay.feature.search"
}

dependencies {
    implementation(project(":data:search"))
}
