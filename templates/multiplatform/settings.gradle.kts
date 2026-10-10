/*
 * Module graph. The dependency direction between the tiers is one-way, and build-logic's
 * ModuleGraph fails the build on an edge that breaks it:
 *
 *   :androidApp, :shared     composition roots: wire everything, own no logic
 *        ↓
 *   :feature:*               presentation only. Never depends on another :feature:*.
 *        ↓
 *   :data:*                  one business domain end to end. Never depends on another :data:*.
 *        ↓
 *   :core:*                  infrastructure. Never depends on :data:* or :feature:*.
 */

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "BaseApp"

include(":androidApp")
include(":shared")

include(":core:common")
include(":core:mvi")
include(":core:designsystem")
include(":core:ui")
include(":core:navigation")
include(":core:datastore")
include(":core:testing")
// <opt:network>
include(":core:network")
// </opt:network>
// <opt:database>
include(":core:database")
// </opt:database>

// <opt:sample>
include(":data:sample")
// </opt:sample>
// <generated:settings-includes-data>

// <opt:sample>
include(":feature:sample")
// </opt:sample>
// <opt:settings>
include(":feature:settings")
// </opt:settings>
// <opt:onboarding>
include(":feature:onboarding")
// </opt:onboarding>
// <generated:settings-includes-feature>
