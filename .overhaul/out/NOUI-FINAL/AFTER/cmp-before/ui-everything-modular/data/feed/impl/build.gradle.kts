plugins {
    id("com.example.cmp.android.data")
}

android {
    namespace = "com.example.cmp.data.feed"
}

dependencies {
    api(project(":data:feed:api"))
    implementation(project(":core:network"))
    // `api`: PagingData is in this module's API. paging-common rather than paging-runtime, which
    // brings RecyclerView and LiveData into a Compose app that uses neither.
    api(libs.androidx.paging.common)
}
