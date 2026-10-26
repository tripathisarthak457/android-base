plugins {
    id("com.example.cmp.android.feature")
}

android {
    namespace = "com.example.cmp.feature.feed"
}

dependencies {
    api(project(":feature:feed:api"))
    implementation(project(":data:feed:api"))

    testImplementation(testFixtures(project(":data:feed:api")))
}
