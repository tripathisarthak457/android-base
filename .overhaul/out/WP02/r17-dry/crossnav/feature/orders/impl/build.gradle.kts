plugins {
    id("com.example.modulescompact.android.feature")
}

android {
    namespace = "com.example.modulescompact.feature.orders"
}

dependencies {
    api(project(":feature:orders:api"))
    implementation(project(":data:orders:api"))

    testImplementation(testFixtures(project(":data:orders:api")))
}
