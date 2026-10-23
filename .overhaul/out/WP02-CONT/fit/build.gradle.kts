buildscript {
    // The generated projects resolve Spotless beside other plugins, which lift its okio; alone it
    // would ask for a version the offline cache does not hold.
    configurations.classpath {
        resolutionStrategy.force("com.squareup.okio:okio:3.9.1", "com.squareup.okio:okio-jvm:3.9.1")
    }
}

plugins {
    id("com.diffplug.spotless") version "8.10.4"
}

repositories {
    mavenCentral()
}

spotless {
    // As the generated projects' .gitattributes asks: LF everywhere, never the platform's ending.
    lineEndings = com.diffplug.spotless.LineEnding.UNIX
    kotlin {
        target("cases/**/*.kt")
        ktlint("1.8.0")
    }
    kotlinGradle {
        target("cases/**/*.gradle.kts")
        ktlint("1.8.0")
    }
}
