plugins {
    id("com.example.finalafter.crui.android.data")
}

android {
    namespace = "com.example.finalafter.crui.data.sample"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:database"))
}
