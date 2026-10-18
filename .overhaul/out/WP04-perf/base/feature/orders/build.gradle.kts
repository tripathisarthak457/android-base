plugins {
    id("com.example.wp04.base.android.feature")
}

android {
    namespace = "com.example.wp04.base.feature.orders"
}

dependencies {
    implementation(project(":data:orders"))
}
