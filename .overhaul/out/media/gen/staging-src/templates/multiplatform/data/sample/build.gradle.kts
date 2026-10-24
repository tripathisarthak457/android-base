plugins {
    id("com.base.app.kmp.data")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.network)
            // <opt:database>
            implementation(projects.core.database)
            // </opt:database>
        }
    }
}
