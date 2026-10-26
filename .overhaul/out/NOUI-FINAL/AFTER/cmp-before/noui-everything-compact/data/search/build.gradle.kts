plugins {
    id("com.example.cmp.android.data")
}

android {
    namespace = "com.example.cmp.data.search"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))
}
