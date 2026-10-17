plugins {
    id("com.example.standardmodular.android.data")
}

android {
    namespace = "com.example.standardmodular.data.auth"
}

dependencies {
    api(project(":data:auth:api"))
    implementation(project(":core:datastore"))
    implementation(project(":core:network"))
}
