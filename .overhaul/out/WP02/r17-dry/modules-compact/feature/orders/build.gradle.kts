plugins {
    id("com.example.modulescompact.android.feature")
}

android {
    namespace = "com.example.modulescompact.feature.orders"
}

dependencies {
    implementation(project(":data:orders"))

    testImplementation(testFixtures(project(":data:orders")))
}
