plugins {
    id("com.base.app.kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.animation)
            implementation(libs.androidx.window.core)
        }
    }
}
