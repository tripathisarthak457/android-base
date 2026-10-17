plugins {
    id("com.example.modulescompact.android.feature")
}

android {
    namespace = "com.example.modulescompact.feature.wallet"
}

dependencies {
    api(project(":feature:wallet:api"))
    implementation(project(":data:wallet:api"))

    testImplementation(testFixtures(project(":data:wallet:api")))
}
