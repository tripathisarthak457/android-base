/*
 * Pure Kotlin on purpose: no Android, no DI annotations, no java.* imports. Every file here compiles
 * unchanged in the multiplatform template's commonMain, and the module graph refuses an Android
 * plugin on it.
 */
plugins {
    id("com.example.lean.jvm.library")
}

dependencies {
    api(libs.kotlinx.coroutines.core)
}
