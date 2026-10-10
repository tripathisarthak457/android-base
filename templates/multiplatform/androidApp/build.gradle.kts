plugins {
    id("com.base.app.android.application")
}

dependencies {
    implementation(projects.shared)
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.android)

    constraints {
        for (navigation3 in listOf(libs.androidx.navigation3.runtime, libs.androidx.navigation3.ui)) {
            addProvider("implementation", navigation3) { version { strictly(libs.versions.androidxNavigation3.get()) } }
        }
    }
}
