/*
 * Module graph.
 *
 * Four tiers, and the dependency direction between them is one-way:
 *
 *   :app / :catalog   composition roots — wire everything, own no logic
 *        ↓
 *   :feature:*        presentation only. Never depends on another :feature:*.
 *        ↓
 *   :data:*           one business domain end to end. Never depends on another :data:*.
 *        ↓
 *   :core:*           infrastructure. Never depends on :data:* or :feature:*.
 *
 * The two "never depends on a sibling" rules are what keep the graph acyclic and the build
 * parallel. They are enforced by the :moduleGraphCheck task rather than by review discipline.
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

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BaseApp"

include(":app")
// <opt:catalog>
include(":catalog")
// </opt:catalog>

// ── core ─────────────────────────────────────────────────────────────────────
include(":core:model")
include(":core:common")
include(":core:coroutines")
include(":core:datastore")
include(":core:designsystem")
include(":core:navigation")
include(":core:ui")
include(":core:testing")
// <opt:network>
include(":core:network")
// </opt:network>
// <opt:push>
include(":core:notification")
// </opt:push>
// <opt:analytics>
include(":core:analytics")
// </opt:analytics>
// <opt:media>
include(":core:media")
// </opt:media>

// ── data ─────────────────────────────────────────────────────────────────────
// <opt:auth>
include(":data:auth")
// </opt:auth>
// <opt:sample>
include(":data:sample")
// </opt:sample>
// <generated:data-modules>

// ── feature ──────────────────────────────────────────────────────────────────
// <opt:auth>
include(":feature:auth")
// </opt:auth>
// <opt:sample>
include(":feature:sample")
// </opt:sample>
// <opt:settings>
include(":feature:settings")
// </opt:settings>
// <opt:onboarding>
include(":feature:onboarding")
// </opt:onboarding>
// <generated:feature-modules>

// ── tooling ──────────────────────────────────────────────────────────────────
// <opt:baselineprofile>
include(":benchmark")
// </opt:baselineprofile>
