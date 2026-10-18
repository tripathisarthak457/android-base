plugins {
    id("com.example.wp04.abstd.android.feature")
}

android {
    namespace = "com.example.wp04.abstd.feature.sample"
}

dependencies {
    implementation(project(":data:sample"))
}
