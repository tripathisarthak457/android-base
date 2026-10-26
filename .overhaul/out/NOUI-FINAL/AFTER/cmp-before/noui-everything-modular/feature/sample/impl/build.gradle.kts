plugins {
    id("com.example.cmp.android.feature")
}

android {
    namespace = "com.example.cmp.feature.sample"
}

dependencies {
    api(project(":feature:sample:api"))
    implementation(project(":data:sample:api"))

    testImplementation(testFixtures(project(":data:sample:api")))
}
