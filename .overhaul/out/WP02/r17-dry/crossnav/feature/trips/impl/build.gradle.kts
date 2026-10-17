plugins {
    id("com.example.modulescompact.android.feature")
}

android {
    namespace = "com.example.modulescompact.feature.trips"
}

dependencies {
    implementation(project(":feature:orders:api"))
    api(project(":feature:trips:api"))
    implementation(project(":data:trips:api"))

    testImplementation(testFixtures(project(":data:trips:api")))
}
