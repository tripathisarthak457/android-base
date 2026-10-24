plugins {
    id("com.base.app.kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.mvi)
            api(projects.core.designsystem)
            implementation(libs.lifecycle.runtime.compose)
            // <opt:coil>
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            // </opt:coil>
        }
    }
}
