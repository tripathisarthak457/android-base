plugins {
    id("com.example.everything.android.data")
}

android {
    namespace = "com.example.everything.data.search"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))
}
