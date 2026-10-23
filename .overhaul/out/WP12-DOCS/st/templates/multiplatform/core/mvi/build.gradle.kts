plugins {
    id("com.base.app.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.common)
            api(libs.lifecycle.viewmodel)
            // UiText names its strings by Compose resource; nothing here composes.
            api(libs.compose.components.resources)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
            implementation(libs.turbine)
        }
    }
}
