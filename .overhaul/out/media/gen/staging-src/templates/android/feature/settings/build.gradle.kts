plugins {
    id("com.base.app.android.feature")
}

android {
    namespace = "com.base.app.feature.settings"
}

dependencies {
    // Settings reads the preference store directly; it is app configuration, not a domain.
    implementation(project(":core:datastore"))
    // <opt:push>
    implementation(project(":core:notification"))
    // </opt:push>
}
