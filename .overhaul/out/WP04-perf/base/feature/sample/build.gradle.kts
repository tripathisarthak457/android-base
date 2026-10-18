plugins {
    id("com.example.wp04.base.android.feature")
}

android {
    namespace = "com.example.wp04.base.feature.sample"
}

dependencies {
    implementation(project(":data:sample"))
}
