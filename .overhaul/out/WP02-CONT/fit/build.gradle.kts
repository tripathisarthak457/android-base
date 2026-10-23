plugins {
    id("com.diffplug.spotless") version "8.10.4"
}

repositories {
    mavenCentral()
}

spotless {
    kotlin {
        target("cases/**/*.kt")
        ktlint("1.8.0")
    }
    kotlinGradle {
        target("cases/**/*.gradle.kts")
        ktlint("1.8.0")
    }
}
