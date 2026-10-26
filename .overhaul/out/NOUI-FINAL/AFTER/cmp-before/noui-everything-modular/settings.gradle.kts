/*
 * Module graph. Four tiers, and the dependency direction between them is one-way:
 *
 *   :app / :catalog   composition roots — wire everything, own no logic
 *        ↓
 *   :feature:*        presentation only. Never depends on another :feature:*.
 *        ↓
 *   :data:*           one business domain end to end. Never depends on another :data:*.
 *        ↓
 *   :core:*           infrastructure. Never depends on :data:* or :feature:*.
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
        // Google's repository is asked only for Google's groups, so a Ktor or Kotlin artifact goes
        // straight to Maven Central instead of a lookup that always misses first.
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

rootProject.name = "FinalAfter"

include(":app")

// ── core ─────────────────────────────────────────────────────────────────────
include(":core:common")
include(":core:mvi")
include(":core:datastore")
include(":core:designsystem")
include(":core:navigation")
include(":core:ui")
include(":core:testing")
include(":core:testing:compose")
include(":core:network")
include(":core:notification")
include(":core:analytics")
include(":core:media")
include(":core:flags")
include(":core:devtools")
include(":core:perfhud")
include(":core:database")
include(":core:applock")
include(":core:playstore")

// ── data ─────────────────────────────────────────────────────────────────────
include(":data:auth:api")
include(":data:auth:impl")
include(":data:sample:api")
include(":data:sample:impl")
include(":data:feed:api")
include(":data:feed:impl")
include(":data:search:api")
include(":data:search:impl")
include(":data:profile:api")
include(":data:profile:impl")
// ── feature ──────────────────────────────────────────────────────────────────
include(":feature:auth:api")
include(":feature:auth:impl")
include(":feature:sample:api")
include(":feature:sample:impl")
include(":feature:feed:api")
include(":feature:feed:impl")
include(":feature:search:api")
include(":feature:search:impl")
include(":feature:profile:api")
include(":feature:profile:impl")
include(":feature:settings:api")
include(":feature:settings:impl")
include(":feature:onboarding:api")
include(":feature:onboarding:impl")
// ── tooling ──────────────────────────────────────────────────────────────────
include(":benchmark")
