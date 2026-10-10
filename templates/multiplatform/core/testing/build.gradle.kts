plugins {
    id("com.base.app.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.common)
            api(libs.kotlinx.coroutines.test)
        }
    }
}
