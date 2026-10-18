plugins {
    id("com.base.app.android.feature")
}

android {
    namespace = "com.base.app.feature.feed"
}

dependencies {
    implementation(project(":data:feed"))
}
