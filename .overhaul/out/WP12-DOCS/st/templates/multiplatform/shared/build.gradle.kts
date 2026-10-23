plugins {
    id("com.base.app.kmp.umbrella")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.designsystem)
            implementation(projects.core.navigation)
            implementation(projects.core.datastore)
            // <opt:network>
            implementation(projects.core.network)
            // </opt:network>
            // <opt:database>
            implementation(projects.core.database)
            // </opt:database>
            // <opt:sample>
            implementation(projects.data.sample)
            implementation(projects.feature.sample)
            // </opt:sample>
            // <opt:settings>
            implementation(projects.feature.settings)
            // </opt:settings>
            // <opt:onboarding>
            implementation(projects.feature.onboarding)
            // </opt:onboarding>
            // <generated:shared-dependencies>
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.kotlinx.serialization.json)
        }
        getByName("androidHostTest").dependencies {
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.test)
            implementation(libs.junit)
        }
    }
}
