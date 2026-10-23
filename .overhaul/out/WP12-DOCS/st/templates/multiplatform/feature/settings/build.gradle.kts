plugins {
    id("com.base.app.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Settings reads the preference store directly: it is app configuration, not a domain.
            implementation(projects.core.datastore)
        }
    }
}
