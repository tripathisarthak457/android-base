plugins {
    id("com.example.finalafter.crnoui3.android.data")
}

android {
    namespace = "com.example.finalafter.crnoui3.data.sample"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:database"))
}
