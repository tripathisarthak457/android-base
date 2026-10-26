package com.example.finalafter.crui.buildlogic.guard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleGraphTest {

    @Test
    fun `a compact feature reaching for another feature names the edge`() {
        assertEquals(
            listOf(":feature:trips -> :feature:orders"),
            violations(":feature:trips", ":core:ui", ":data:trips", ":feature:orders"),
        )
    }

    @Test
    fun `a compact data module may not see another data module`() {
        assertEquals(listOf(":data:trips -> :data:orders"), violations(":data:trips", ":core:network", ":data:orders"))
    }

    @Test
    fun `core never sees data or features`() {
        assertEquals(
            listOf(":core:ui -> :data:orders", ":core:ui -> :feature:orders"),
            violations(":core:ui", ":core:designsystem", ":data:orders", ":feature:orders"),
        )
    }

    @Test
    fun `a nested core module is core, and core is all it may see`() {
        assertEquals(ModuleKind.CORE, ModuleShape.of(":core:testing:compose")?.kind)
        assertEquals(
            listOf(":core:testing:compose -> :feature:orders"),
            violations(":core:testing:compose", ":core:designsystem", ":core:testing", ":feature:orders"),
        )
    }

    @Test
    fun `a feature impl sees other features only through their api`() {
        assertEquals(
            listOf(":feature:trips:impl -> :feature:orders:impl", ":feature:trips:impl -> :data:orders:impl"),
            violations(
                ":feature:trips:impl",
                ":feature:trips:api", ":feature:orders:api", ":data:orders:api", ":domain:pricing", ":core:ui",
                ":feature:orders:impl", ":data:orders:impl",
            ),
        )
    }

    @Test
    fun `an api module sees only core common`() {
        assertEquals(
            listOf(":feature:orders:api -> :core:navigation", ":feature:orders:api -> :data:orders:api"),
            violations(":feature:orders:api", ":core:common", ":core:navigation", ":data:orders:api"),
        )
        assertEquals(listOf(":data:orders:api -> :core:network"), violations(":data:orders:api", ":core:common", ":core:network"))
    }

    @Test
    fun `a data impl sees its own api and core, and nothing of another domain`() {
        assertEquals(
            listOf(":data:trips:impl -> :data:orders:api"),
            violations(":data:trips:impl", ":data:trips:api", ":core:network", ":data:orders:api"),
        )
    }

    @Test
    fun `a domain module sees data apis and core common only`() {
        assertEquals(
            listOf(":domain:pricing -> :data:orders:impl", ":domain:pricing -> :core:network"),
            violations(":domain:pricing", ":data:orders:api", ":core:common", ":data:orders:impl", ":core:network"),
        )
    }

    @Test
    fun `pure modules are recognised by path`() {
        val pure = listOf(":core:common", ":feature:orders:api", ":data:orders:api", ":domain:pricing")
        val android = listOf(":core:ui", ":feature:orders", ":feature:orders:impl", ":data:orders", ":data:orders:impl")

        assertTrue(pure.all { ModuleShape.of(it)!!.kind.pure })
        assertTrue(android.none { ModuleShape.of(it)!!.kind.pure })
    }

    @Test
    fun `the design system sees no other module but the test helpers`() {
        assertEquals(ModuleKind.DESIGN_SYSTEM, ModuleShape.of(":core:designsystem")?.kind)
        assertEquals(
            listOf(":core:designsystem -> :core:common", ":core:designsystem -> :core:network"),
            violations(":core:designsystem", ":core:testing", ":core:testing:compose", ":core:common", ":core:network"),
        )
    }

    @Test
    fun `the design system's libraries are Compose, the window, its UI peers and the screenshot test stack`() {
        val allowed = listOf(
            "androidx.compose:compose-bom",
            "androidx.compose.foundation:foundation",
            "androidx.compose.ui:ui-text-google-fonts",
            "androidx.compose.ui:ui-test-junit4",
            "androidx.window:window-core",
            "androidx.navigationevent:navigationevent-compose",
            "androidx.lifecycle:lifecycle-runtime-compose",
            "androidx.activity:activity-compose",
            "io.github.kyant0:backdrop",
            "org.jetbrains.kotlin:kotlin-stdlib",
            "junit:junit",
            "org.robolectric:robolectric",
            "io.github.takahirom.roborazzi:roborazzi-accessibility-check",
            "io.github.sergio-sastre.ComposablePreviewScanner:android",
            "com.google.android.apps.common.testing.accessibility.framework:accessibility-test-framework",
        )
        val refused = listOf(
            "androidx.lifecycle:lifecycle-viewmodel-compose",
            "androidx.hilt:hilt-navigation-compose",
            "io.ktor:ktor-client-core",
            "androidx.room3:room3-runtime",
            "io.coil-kt.coil3:coil-compose",
            // Compose, but not the design system's: it draws its own components.
            "androidx.compose.material3:material3",
            "androidx.compose.material:material-icons-core",
        )
        val designSystem = ModuleShape.of(":core:designsystem")!!

        assertEquals(
            refused.map { ":core:designsystem -> $it" },
            designSystem.libraryViolations(":core:designsystem", allowed + refused),
        )
        assertEquals(emptyList<String>(), ModuleShape.of(":core:ui")!!.libraryViolations(":core:ui", refused))
    }

    @Test
    fun `nothing but the app and the catalog composes the navigation shell`() {
        val shell = ":core:navigation:shell"

        assertEquals(listOf(":feature:orders -> $shell"), violations(":feature:orders", ":core:navigation", shell))
        assertEquals(
            listOf(":feature:orders:impl -> $shell"),
            violations(":feature:orders:impl", ":feature:orders:api", ":core:navigation", shell),
        )
        assertEquals(listOf(":data:orders -> $shell"), violations(":data:orders", ":core:network", shell))
        assertEquals(listOf(":data:orders:impl -> $shell"), violations(":data:orders:impl", ":data:orders:api", shell))
        assertEquals(listOf(":domain:pricing -> $shell"), violations(":domain:pricing", ":core:common", shell))
        assertEquals(emptyList<String>(), violations(shell, ":core:navigation", ":core:designsystem"))
    }

    @Test
    fun `the navigation runtime never sees the shell built on it`() {
        assertEquals(
            listOf(":core:navigation -> :core:navigation:shell"),
            violations(":core:navigation", ":core:common", ":core:navigation:shell"),
        )
    }

    @Test
    fun `the navigation runtime never sees the design system, so a feature does not either`() {
        assertEquals(
            listOf(":core:navigation -> :core:designsystem"),
            violations(":core:navigation", ":core:common", ":core:designsystem"),
        )
    }

    @Test
    fun `the composition roots are not checked`() {
        assertNull(ModuleShape.of(":app"))
        assertNull(ModuleShape.of(":catalog"))
        assertNull(ModuleShape.of(":benchmark"))
    }

    private fun violations(module: String, vararg dependencies: String): List<String> =
        ModuleShape.of(module)!!.violations(module, dependencies.toList())
}
