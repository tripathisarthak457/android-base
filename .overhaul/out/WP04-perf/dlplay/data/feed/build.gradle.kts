plugins {
    id("com.example.wp04.dlplay.android.data")
}

android {
    namespace = "com.example.wp04.dlplay.data.feed"
}

dependencies {
    // paging-runtime is not Compose: the data module builds the pager, the screen collects it.
    api(libs.androidx.paging.runtime)
}
