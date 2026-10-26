plugins {
    id("com.example.finalafter.crui2.android.data")
}

android {
    namespace = "com.example.finalafter.crui2.data.sample"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:database"))
}
