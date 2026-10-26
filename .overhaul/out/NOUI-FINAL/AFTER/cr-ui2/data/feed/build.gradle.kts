plugins {
    id("com.example.finalafter.crui2.android.data")
}

android {
    namespace = "com.example.finalafter.crui2.data.feed"
}

dependencies {
    implementation(project(":core:network"))
    // `api`: PagingData is in this module's API. paging-common rather than paging-runtime, which
    // brings RecyclerView and LiveData into a Compose app that uses neither.
    api(libs.androidx.paging.common)
}
