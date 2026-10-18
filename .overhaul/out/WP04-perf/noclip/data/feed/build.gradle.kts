plugins {
    id("com.example.wp04.everything.android.data")
}

android {
    namespace = "com.example.wp04.everything.data.feed"
}

dependencies {
    // paging-runtime is not Compose: the data module builds the pager, the screen collects it.
    api(libs.androidx.paging.runtime)
}
