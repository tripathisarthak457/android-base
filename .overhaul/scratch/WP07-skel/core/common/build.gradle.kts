/*
 * Pure Kotlin: no Android, no Compose, no DI. The sources are the Android template's :core:common
 * file for file, which tools/sync_check.py holds to.
 */
plugins {
    id("com.base.app.kmp.pure")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
        }
    }
}
