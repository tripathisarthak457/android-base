/*
 * Rules that span more than one data domain, in plain Kotlin: a use case that reads two
 * repositories, a policy several features share. Name the data :api modules it reads; features
 * that need the rules depend on this module. Nothing here may know about Android or Compose.
 */
plugins {
    id("${package}.jvm.library")
}

dependencies {
    api(project(":core:common"))
}
