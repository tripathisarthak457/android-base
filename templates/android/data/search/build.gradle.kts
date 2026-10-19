plugins {
    id("com.base.app.android.data")
}

android {
    namespace = "com.base.app.data.search"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))
}
