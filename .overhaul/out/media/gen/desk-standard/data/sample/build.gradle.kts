plugins {
    id("com.example.deskstandard.android.data")
}

android {
    namespace = "com.example.deskstandard.data.sample"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:database"))
}
