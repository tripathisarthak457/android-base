package com.base.app.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * The conventions this project holds, checked rather than remembered.
 *
 * Every rule here was chosen the same way: it describes a mistake that compiles, that passes
 * review because the diff looks reasonable in isolation, and that is expensive to undo a year
 * later. A rule that merely encodes a taste is deliberately absent — the point is for this suite
 * to be trusted, and one that fails on something arguable is one people learn to add `@Ignore` to.
 *
 * These do not overlap the two guards the build already has. `verifyModuleDependencies` polices
 * the *edges* of the module graph and `verifyComposeUsage` polices Material imports and
 * untranslated copy; neither can see inside a class, which is where every rule below lives.
 *
 * Konsist reads Kotlin source rather than bytecode, so this module needs neither the Android SDK
 * nor any of the modules it inspects on its classpath, and the whole suite runs in seconds.
 *
 * Scoped to production sources throughout. A test may hold an Activity, name a class
 * `FakeThingRepository` and block a thread on purpose; none of that ships.
 */
class ArchitectureTest {

    /**
     * A ViewModel holding an Activity outlives the thing it is holding.
     *
     * It survives configuration change and the Activity does not, so from the moment the device
     * is rotated the reference is to a destroyed one: leaked memory at best, and at worst a
     * resource lookup against a context that no longer has a configuration. It is also how
     * untestable ViewModels happen, because constructing one in a test then needs Robolectric.
     *
     * `android.content.Context` is not on the list on purpose: `@ApplicationContext Context` is
     * the correct way to reach resources from a ViewModel and banning the import would ban that
     * too. What is banned is the types that cannot be anything but a leak.
     */
    @Test
    fun `a ViewModel never holds an Activity`() {
        Konsist.scopeFromProduction()
            .classes()
            .withNameEndingWith("ViewModel")
            .assertTrue(additionalMessage = LEAKED_CONTEXT) { viewModel ->
                viewModel.containingFile.imports.none { it.name in BANNED_IN_VIEW_MODELS }
            }
    }

    /**
     * Every ViewModel is an MVI one.
     *
     * The base class is where `launchLatest`, `persistState`, effect delivery and the
     * Loading/Refreshing distinction live. A ViewModel extending `androidx.lifecycle.ViewModel`
     * directly compiles perfectly well and then reimplements a worse version of each of them, one
     * screen at a time — which is exactly how a codebase ends up with four state conventions and
     * no way to tell which one is current.
     */
    @Test
    fun `every ViewModel extends the MVI base`() {
        Konsist.scopeFromProduction()
            .classes()
            .withNameEndingWith("ViewModel")
            // The base class is the one class that legitimately does not extend itself.
            .filterNot { it.name == MVI_BASE }
            .assertTrue(additionalMessage = NOT_MVI) { viewModel ->
                // Matched by prefix rather than by name: the parent as written carries its three
                // type arguments — `MviViewModel<SampleState, SampleEvent, SampleEffect>` — and an
                // exact-name check matches none of them.
                viewModel.hasParent { it.name.startsWith(MVI_BASE) }
            }
    }

    /**
     * A repository is an interface with one `Default` implementation behind it.
     *
     * Not a style preference: it is what lets a ViewModel test inject a fake instead of opening a
     * socket. A concrete-only repository forces every test that touches it into Robolectric and a
     * mocking framework, and those tests stop being written shortly afterwards.
     */
    @Test
    fun `a repository is an interface with a Default implementation`() {
        val classNames = Konsist.scopeFromProduction()
            .classes()
            .map { it.name }
            .toSet()

        Konsist.scopeFromProduction()
            .interfaces()
            .withNameEndingWith("Repository")
            .assertTrue(additionalMessage = NO_IMPLEMENTATION) { repository ->
                "Default${repository.name}" in classNames
            }
    }

    /**
     * A data module never sees Compose.
     *
     * The module-graph check cannot catch this one: `:data:*` is allowed to depend on `:core:*`
     * and `:core:ui` is a core module, so a data module can reach Compose through an entirely
     * legal edge. What arrives with it is a repository returning a `Color` or an
     * `AnnotatedString` — a presentation decision made one layer too low, and then impossible to
     * change without editing the layer that talks to the network.
     */
    @Test
    fun `a data module never imports Compose`() {
        Konsist.scopeFromProduction()
            .files
            .filter(::inDataModule)
            .assertTrue(additionalMessage = COMPOSE_IN_DATA) { file ->
                file.imports.none { it.name.startsWith("androidx.compose") }
            }
    }

    /**
     * The one rule here that narrows before it asserts, checked against a second opinion.
     *
     * A rule that filters can pass by selecting nothing, and that is indistinguishable from
     * passing honestly — which is exactly what happened: the first version of the rule above
     * matched on a path separator, so on Windows it read no files and went green, and only CI on
     * Linux disagreed. Asking the same question a different way is what makes that visible.
     *
     * The package is an independent answer to "is this a data module": Konsist reads it from the
     * `package` line rather than from the filesystem, so a separator cannot affect it. The two
     * need not agree on a count — they will not — but if one finds files and the other finds none,
     * one of them is broken.
     */
    @Test
    fun `the data module rule is reading the data modules`() {
        val files = Konsist.scopeFromProduction().files
        val byPackage = files.count { it.packagee?.name?.contains(".data.") == true }
        val bySelector = files.count(::inDataModule)

        assertEquals(
            "The data-module rule selected $bySelector files and the package says there are " +
                "$byPackage. When one of those is zero and the other is not, the rule above is " +
                "passing without reading anything.",
            byPackage == 0,
            bySelector == 0,
        )
    }

    /**
     * `runBlocking` never ships.
     *
     * It parks the calling thread until the coroutine finishes. On the main thread that is a
     * frozen frame and, past five seconds, an ANR — and the call site is almost always a
     * repository method somebody needed from a non-suspending place at four in the afternoon. In
     * a test it is entirely correct, which is why this is scoped to production source.
     */
    @Test
    fun `production code never blocks a thread on a coroutine`() {
        Konsist.scopeFromProduction()
            .files
            .assertTrue(additionalMessage = RUN_BLOCKING) { file ->
                file.imports.none { it.name == "kotlinx.coroutines.runBlocking" }
            }
    }

    /**
     * A screen composable never takes a ViewModel.
     *
     * The project's shape is a stateless screen plus a small route that owns the ViewModel and
     * hands it down as state and callbacks — which is what makes every screen previewable and
     * testable without Hilt. A `ViewModel` parameter on the screen itself quietly removes both,
     * and the preview that used to work is deleted rather than fixed.
     */
    @Test
    fun `a screen composable takes state, not a ViewModel`() {
        Konsist.scopeFromProduction()
            .functions()
            .withNameEndingWith("Screen")
            .assertTrue(additionalMessage = VIEW_MODEL_IN_SCREEN) { screen ->
                screen.parameters.none { it.type.name.endsWith("ViewModel") }
            }
    }

    /**
     * Whether a file belongs to the `:data:` tier.
     *
     * `moduleName` is the module's own path — `data/sample`, `core/designsystem` — so its first
     * segment is the layer. Normalised because Konsist reports it in the platform's separator,
     * and a rule that answers differently on a laptop than on CI is worse than no rule: it is
     * the laptop that people believe.
     */
    private fun inDataModule(file: KoFileDeclaration): Boolean =
        file.moduleName.replace(File.separatorChar, '/').startsWith(DATA_MODULES)

    private companion object {
        const val MVI_BASE = "MviViewModel"

        /**
         * The `:data:` tier, as a module path rather than a substring of a file path.
         *
         * The first version of this rule asked whether a file's path contained "/data/", and got
         * two things wrong at once. It matched
         * `core/designsystem/component/data/AppDataDisplay.kt`, a design-system component that
         * imports Compose because that is what it is for. And Konsist reports paths in the
         * operating system's own separator, so on Windows the filter matched nothing at all, the
         * rule examined no files, and it passed — which looks exactly like passing honestly. It
         * failed the moment CI ran it on Linux.
         *
         * `moduleName` is the module's own path (`data/sample`, `core/designsystem`), so matching
         * its first segment asks the question the layering is actually about. The separator is
         * still the platform's, hence the replace: a rule that answers differently on a laptop
         * than on CI is worse than no rule, because it is the laptop that people believe.
         */
        const val DATA_MODULES = "data/"

        val BANNED_IN_VIEW_MODELS = setOf(
            "android.app.Activity",
            "android.content.ContextWrapper",
            "android.view.View",
            "androidx.activity.ComponentActivity",
            "androidx.fragment.app.Fragment",
        )

        const val LEAKED_CONTEXT =
            "A ViewModel outlives the Activity that created it, so this reference is to a " +
                "destroyed one the moment the device is rotated. Inject what is actually needed " +
                "instead: @ApplicationContext for resources, an effect for anything that has to " +
                "reach the Activity."

        const val NOT_MVI =
            "Extend MviViewModel from :core:common. It supplies the state/event/effect wiring, " +
                "launchLatest, persistState, and the Loading/Refreshing distinction that stops a " +
                "refresh blanking the screen it is refreshing."

        const val NO_IMPLEMENTATION =
            "Every repository interface needs a Default<Name>Repository bound in its module's " +
                "Hilt module. If this one is genuinely interface-only it is a collaborator rather " +
                "than a repository — give it the name of what it does."

        const val COMPOSE_IN_DATA =
            "A data module returning Compose types decides how something looks one layer below " +
                "the layer that draws it. Return a model and map it in the feature module."

        const val RUN_BLOCKING =
            "runBlocking parks the calling thread until the coroutine finishes — on the main " +
                "thread that is a frozen frame and, past five seconds, an ANR. Make the function " +
                "suspend, or expose a Flow."

        const val VIEW_MODEL_IN_SCREEN =
            "Keep the screen stateless: take the state and the callbacks it needs. The route " +
                "beside it owns the ViewModel. That split is the only reason @Preview works on " +
                "every screen in this project."
    }
}
