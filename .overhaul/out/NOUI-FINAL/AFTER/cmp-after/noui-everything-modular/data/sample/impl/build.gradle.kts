plugins {
    id("com.example.cmp.android.data")
}

android {
    namespace = "com.example.cmp.data.sample"
}

dependencies {
    api(project(":data:sample:api"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
}
