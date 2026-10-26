plugins {
    id("com.example.finalafter.crnoui.android.data")
}

android {
    namespace = "com.example.finalafter.crnoui.data.search"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))
}
