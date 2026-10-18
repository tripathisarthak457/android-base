plugins {
    id("com.example.wp04.base.jvm.library")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
