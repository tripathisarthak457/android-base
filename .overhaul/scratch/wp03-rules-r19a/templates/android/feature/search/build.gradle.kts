plugins {
    id("com.base.app.android.feature")
}

android {
    namespace = "com.base.app.feature.search"
}

dependencies {
    implementation(project(":data:search"))
}
