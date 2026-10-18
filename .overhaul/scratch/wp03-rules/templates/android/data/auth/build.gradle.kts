plugins {
    id("com.base.app.android.data")
}

android {
    namespace = "com.base.app.data.auth"
}

dependencies {
    implementation(project(":core:datastore"))
    implementation(project(":core:network"))
}
