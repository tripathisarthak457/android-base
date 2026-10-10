plugins {
    id("com.base.app.kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.common)
            implementation(projects.core.designsystem)
            implementation(libs.compose.animation)
            api(libs.navigation3.ui)
            implementation(libs.lifecycle.viewmodel.navigation3)
            implementation(libs.lifecycle.runtime.compose)
            api(libs.kotlinx.serialization.json)
        }
    }
}
