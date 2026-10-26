plugins {
    id("com.example.cmp.android.data")
}

android {
    namespace = "com.example.cmp.data.search"
}

dependencies {
    api(project(":data:search:api"))
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))
}
