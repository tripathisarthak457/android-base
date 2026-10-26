plugins {
    id("com.example.cmp.android.feature")
}

android {
    namespace = "com.example.cmp.feature.search"
}

dependencies {
    api(project(":feature:search:api"))
    implementation(project(":data:search:api"))

    testImplementation(testFixtures(project(":data:search:api")))
}
