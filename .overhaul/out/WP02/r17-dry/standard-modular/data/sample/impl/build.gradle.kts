plugins {
    id("com.example.standardmodular.android.data")
}

android {
    namespace = "com.example.standardmodular.data.sample"
}

dependencies {
    api(project(":data:sample:api"))
    implementation(project(":core:network"))
}
