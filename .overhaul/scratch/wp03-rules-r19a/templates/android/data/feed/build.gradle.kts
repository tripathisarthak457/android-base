plugins {
    id("com.base.app.android.data")
}

android {
    namespace = "com.base.app.data.feed"
}

dependencies {
    implementation(project(":core:network"))
    // The repository returns PagingData, so callers see paging-common; paging-compose stays in
    // the UI, and paging-runtime's RecyclerView adapters are not needed at all.
    api(libs.androidx.paging.common)
}
