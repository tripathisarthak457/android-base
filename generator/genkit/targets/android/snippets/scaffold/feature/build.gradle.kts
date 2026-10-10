plugins {
    id("__PKG__.android.feature")
}

android {
    namespace = "__PKG__.feature.__NAME__"
}

dependencies {
    implementation(project(":data:__NAME__"))
}
