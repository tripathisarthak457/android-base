package com.base.app.buildlogic.guard

import com.base.app.buildlogic.guard.CodeRules.Location
import com.base.app.buildlogic.guard.CodeRules.Rule

/**
 * Rules for how a module is organised: packages named for what they hold, a feature's public
 * surface limited to its `api` package, and the immutability the Compose stability config promises.
 */
internal object StructureRules {

    private const val API_DIRECTORY = "/api/"
    private val STATE_FILE = Regex("""\w*(?:Contract|State)\.kt""")
    private val MUTABLE = Regex(
        """\bvar\b|\b(?:MutableList|MutableMap|MutableSet|ArrayList|HashMap|HashSet|LinkedHashMap|LinkedHashSet)\b|""" +
            """\bmutable(?:List|Map|Set)Of\s*\(""",
    )
    private val PACKAGE = Regex("""^\s*package\s+([\w.]+)""")
    private val LAYER_NAMES = setOf(
        "ui", "viewmodel", "presentation", "screens", "util", "utils", "helper", "helpers", "misc", "base", "common",
    )

    val ALL: List<Rule> = listOf(
        Rule(
            id = "layer-package",
            advice = "name a package for the screen or concept it holds (list, detail, checkout, component), not a layer",
            applies = { it.inFeature || it.inData || it.inDesignSystem || it.inNavigation },
            lines = { source, location -> layerPackage(source, location) },
        ),
        Rule(
            id = "feature-public",
            advice = "only a feature's api package is public; make this internal (Hilt modules and ViewModels can be)",
            applies = { it.inFeature && it.isProduction && API_DIRECTORY !in it.inSourceSet },
            lines = { source, _ ->
                Declarations.topLevel(source)
                    .filter { "internal" !in it.modifiers && "private" !in it.modifiers }
                    .map { it.line }
            },
        ),
        Rule(
            id = "uistate-mutable",
            advice = "UI state is read-only: List, Map and Set, val properties, copy() to change; the stability config " +
                "promises the compiler it never mutates",
            applies = { it.inFeature && it.isProduction && STATE_FILE.matches(it.fileName) },
            lines = CodeRules.pattern(MUTABLE),
        ),
        Rule(
            id = "api-immutable",
            advice = "models in an api package are immutable: val properties, read-only collections, no arrays",
            applies = { it.inData && it.isProduction && API_DIRECTORY in it.inSourceSet },
            lines = CodeRules.pattern(Regex("""${MUTABLE.pattern}|\bArray<|\b(?:Int|Long|Short|Byte|Char|Float|Double|Boolean)Array\b""")),
        ),
        Rule(
            id = "hardcoded-copy",
            advice = "move the copy to src/main/res/values/strings.xml and read it with stringResource or UiText",
            applies = { it.sourceSet == "main" && (it.inFeature || it.inApp || it.inDesignSystem || it.inCoreUi) },
            lines = { source, _ -> HardcodedCopy.lines(source) },
        ),
    )

    /**
     * Package segments below the module's own root package. The root is found by the module's
     * place in the project (`feature/orders` → `….feature.orders`), so the project's own package,
     * whatever it is called, is never judged.
     */
    private fun layerPackage(source: KotlinSource, location: Location): List<Int> {
        val parts = location.module.split('/')
        val root = when {
            location.inFeature || location.inData -> parts.take(2)
            else -> parts
        }.joinToString(".")
        source.codeLines.forEachIndexed { index, line ->
            val name = PACKAGE.find(line)?.groupValues?.get(1) ?: return@forEachIndexed
            val below = name.substringAfter(".$root.", missingDelimiterValue = "")
            if (below.isEmpty()) return emptyList()
            return if (below.split('.').any { it in LAYER_NAMES }) listOf(index + 1) else emptyList()
        }
        return emptyList()
    }
}
