plugins {
    id("com.base.app.android.data")
}

android {
    namespace = "com.base.app.data.feed"
}

dependencies {
    // paging-runtime is not Compose: the data module builds the pager, the screen collects it.
    api(libs.androidx.paging.runtime)
}
