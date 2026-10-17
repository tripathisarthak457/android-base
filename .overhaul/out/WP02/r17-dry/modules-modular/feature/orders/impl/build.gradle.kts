plugins {
    id("com.example.modulesmodular.android.feature")
}

android {
    namespace = "com.example.modulesmodular.feature.orders"
}

dependencies {
    api(project(":feature:orders:api"))
    implementation(project(":data:orders:api"))

    testImplementation(testFixtures(project(":data:orders:api")))
}
