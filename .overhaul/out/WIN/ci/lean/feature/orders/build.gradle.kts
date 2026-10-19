plugins {
    id("com.example.cicheck.android.feature")
}

android {
    namespace = "com.example.cicheck.feature.orders"
}

dependencies {
    implementation(project(":data:orders"))
}
