plugins {
    id("com.example.standard.android.data")
}

android {
    namespace = "com.example.standard.data.auth"
}

dependencies {
    implementation(project(":core:datastore"))
    implementation(project(":core:network"))
}
