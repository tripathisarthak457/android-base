plugins {
    id("com.base.app.android.feature")
}

android {
    namespace = "com.base.app.feature.sample"
}

dependencies {
    implementation(project(":data:sample"))
}
