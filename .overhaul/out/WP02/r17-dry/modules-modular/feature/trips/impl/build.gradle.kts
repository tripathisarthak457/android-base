plugins {
    id("com.example.modulesmodular.android.feature")
}

android {
    namespace = "com.example.modulesmodular.feature.trips"
}

dependencies {
    api(project(":feature:trips:api"))
    implementation(project(":data:trips:api"))

    testImplementation(testFixtures(project(":data:trips:api")))
}
