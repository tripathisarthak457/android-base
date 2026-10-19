plugins {
    id("com.base.app.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // The completion flag is a preference, not a domain fact: there is no repository worth
            // putting in front of one boolean that only this screen writes.
            implementation(projects.core.datastore)
        }
    }
}
