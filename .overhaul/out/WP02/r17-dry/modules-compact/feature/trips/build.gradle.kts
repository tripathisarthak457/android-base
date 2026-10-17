plugins {
    id("com.example.modulescompact.android.feature")
}

android {
    namespace = "com.example.modulescompact.feature.trips"
}

dependencies {
    implementation(project(":feature:orders"))
    implementation(project(":data:trips"))

    testImplementation(testFixtures(project(":data:trips")))
}
