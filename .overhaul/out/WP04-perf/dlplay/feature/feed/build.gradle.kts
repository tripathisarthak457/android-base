plugins {
    id("com.example.wp04.dlplay.android.feature")
}

android {
    namespace = "com.example.wp04.dlplay.feature.feed"
}

dependencies {
    implementation(project(":data:feed"))
}
