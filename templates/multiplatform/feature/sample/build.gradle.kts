plugins {
    id("com.base.app.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.data.sample)
        }
    }
}
