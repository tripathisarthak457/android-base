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
        google()
        mavenCentral()
    }
}

rootProject.name = "Lean"

include(":app")
include(":catalog")

// ── core ─────────────────────────────────────────────────────────────────────
include(":core:common")
include(":core:mvi")
include(":core:datastore")
include(":core:designsystem")
include(":core:navigation")
include(":core:ui")
include(":core:testing")
include(":core:network")

// ── data ─────────────────────────────────────────────────────────────────────
include(":data:sample")

// ── feature ──────────────────────────────────────────────────────────────────
include(":feature:sample")

// ── tooling ──────────────────────────────────────────────────────────────────
