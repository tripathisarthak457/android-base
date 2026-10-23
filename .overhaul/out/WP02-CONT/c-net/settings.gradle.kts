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

rootProject.name = "FitCNet"

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

// ── data ─────────────────────────────────────────────────────────────────────
include(":data:abc")
include(":data:abcdefghi")
include(":data:abcdefghijklmno")
include(":data:abcdefghijklmnopqrs")
include(":data:abcdefghijklmnopqrstuvw")
include(":data:abcdefghijklmnopqrstuvwxyzabcd")

// ── feature ──────────────────────────────────────────────────────────────────
include(":feature:abc")
include(":feature:abcdefghi")
include(":feature:abcdefghijklmno")
include(":feature:abcdefghijklmnopqrs")
include(":feature:abcdefghijklmnopqrstuvw")
include(":feature:abcdefghijklmnopqrstuvwxyzabcd")

// ── tooling ──────────────────────────────────────────────────────────────────
