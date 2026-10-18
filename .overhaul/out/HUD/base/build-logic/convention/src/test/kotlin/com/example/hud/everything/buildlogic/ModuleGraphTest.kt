package com.example.hud.everything.buildlogic

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
    fun `the composition roots are not checked`() {
        assertNull(ModuleShape.of(":app"))
        assertNull(ModuleShape.of(":catalog"))
        assertNull(ModuleShape.of(":benchmark"))
    }

    private fun violations(module: String, vararg dependencies: String): List<String> =
        ModuleShape.of(module)!!.violations(module, dependencies.toList())
}
