plugins {
    id("com.example.wp04.everything.android.feature")
}

android {
    namespace = "com.example.wp04.everything.feature.sample"
}

dependencies {
    implementation(project(":data:sample"))
}
