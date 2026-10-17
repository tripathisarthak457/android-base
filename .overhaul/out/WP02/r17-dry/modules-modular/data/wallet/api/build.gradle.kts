/*
 * This domain's public surface: the repository interface and the models it returns. Pure Kotlin,
 * so features compile against it without the network, storage or DI code behind it.
 */
plugins {
    id("com.example.modulesmodular.jvm.library")
}

dependencies {
    api(project(":core:common"))
}
