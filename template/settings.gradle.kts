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
// <opt:flags>
include(":core:flags")
// </opt:flags>
// <opt:devtools>
include(":core:devtools")
// </opt:devtools>
// <opt:database>
include(":core:database")
// </opt:database>

// ── data ─────────────────────────────────────────────────────────────────────
// <opt:auth>
include(":data:auth")
// </opt:auth>
// <opt:sample>
include(":data:sample")
// </opt:sample>
// <opt:paging>
include(":data:feed")
// </opt:paging>
// <opt:search>
include(":data:search")
// </opt:search>
// <opt:profile>
include(":data:profile")
// </opt:profile>
// <generated:data-modules>

// ── feature ──────────────────────────────────────────────────────────────────
// <opt:auth>
include(":feature:auth")
// </opt:auth>
// <opt:sample>
include(":feature:sample")
// </opt:sample>
// <opt:paging>
include(":feature:feed")
// </opt:paging>
// <opt:search>
include(":feature:search")
// </opt:search>
// <opt:profile>
include(":feature:profile")
// </opt:profile>
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
