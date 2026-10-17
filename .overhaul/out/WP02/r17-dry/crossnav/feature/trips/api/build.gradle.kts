/*
 * This feature's public surface: its navigation keys and result contracts. Pure Kotlin, so a
 * feature that opens one of its screens compiles against these keys alone and never waits for
 * the feature's own code to build.
 */
plugins {
    id("com.example.modulescompact.jvm.library")
}

dependencies {
    api(project(":core:common"))
    api(libs.kotlinx.serialization.core)
}
