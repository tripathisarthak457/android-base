plugins {
    id("com.base.app.android.data")
}

android {
    namespace = "com.base.app.data.sample"
}

dependencies {
    implementation(project(":core:network"))
}
