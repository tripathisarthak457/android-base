plugins {
    id("com.example.standardmodular.android.feature")
}

android {
    namespace = "com.example.standardmodular.feature.sample"
}

dependencies {
    api(project(":feature:sample:api"))
    implementation(project(":data:sample:api"))
}
