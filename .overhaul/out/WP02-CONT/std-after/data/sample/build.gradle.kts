plugins {
    id("com.example.standard.android.data")
}

android {
    namespace = "com.example.standard.data.sample"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:database"))
}
