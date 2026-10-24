plugins {
    id("com.example.everything.android.data")
}

android {
    namespace = "com.example.everything.data.sample"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:database"))
}
