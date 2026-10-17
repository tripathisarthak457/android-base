plugins {
    id("com.example.modulesmodular.android.feature")
}

android {
    namespace = "com.example.modulesmodular.feature.wallet"
}

dependencies {
    api(project(":feature:wallet:api"))
    implementation(project(":data:wallet:api"))

    testImplementation(testFixtures(project(":data:wallet:api")))
}
