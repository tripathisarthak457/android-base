/*
 * The rules about how code is written, as tests.
 *
 * Deliberately a plain JVM module with no Android in it: Konsist reads Kotlin *source*, not
 * compiled classes, so this module needs neither the SDK nor any of the modules it inspects on
 * its classpath. That is what keeps the whole suite a couple of seconds and lets it run on any
 * machine, including one with no Android SDK at all.
 *
 * This does not overlap the two guards that already exist. `verifyModuleDependencies` polices the
 * *edges* of the module graph and `verifyComposeUsage` polices Material imports and untranslated
 * copy. Neither can see inside a class, which is where every rule in ArchitectureTest lives.
 */
plugins {
    id("com.base.app.jvm.library")
}

dependencies {
    testImplementation(libs.konsist)
}
