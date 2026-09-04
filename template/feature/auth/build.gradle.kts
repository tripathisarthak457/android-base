plugins {
    id("com.base.app.android.feature")
}

android {
    namespace = "com.base.app.feature.auth"
}

dependencies {
    implementation(project(":data:auth"))
}
