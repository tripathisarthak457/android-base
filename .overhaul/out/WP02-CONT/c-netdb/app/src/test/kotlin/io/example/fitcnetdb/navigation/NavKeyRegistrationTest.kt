package io.example.fitcnetdb.navigation

import io.example.fitcnetdb.core.common.navigation.AppNavKey
import io.example.fitcnetdb.core.navigation.NavKeySerialization
import io.example.fitcnetdb.ui.AppDestinations
import java.io.File
import java.lang.reflect.Modifier
import java.util.jar.JarFile
import kotlin.reflect.KClass
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleCollector
import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every navigation key in the app survives the saved state, written with the [NavKeySerialization]
 * the back stacks use. The keys are found on the classpath rather than listed here, so a key left
 * out of its feature's `navKeys { }` block fails this test instead of sending a user who comes back
 * from the background to Home.
 */
@OptIn(ExperimentalSerializationApi::class)
class NavKeyRegistrationTest {
    private val classes = appClasses()
    private val keyTypes = classes.filter(::isConcreteNavKey).toSet()
    private val serialization = NavKeySerialization(classes.flatMap(::navKeyModules).toSet())
    private val stack = ListSerializer(PolymorphicSerializer(AppNavKey::class))

    @Test
    fun `the scan sees the app's keys`() {
        // A classpath the scan cannot read would pass the other two tests with nothing checked.
        assertTrue("The start destination was not found among ${keyTypes.size} keys", AppDestinations.start::class.java in keyTypes)
    }

    @Test
    fun `every key is registered for restore`() {
        val registered = serialization.json.serializersModule.navKeyTypes()
        val missing = (keyTypes - registered).map { it.name }.sorted()

        assertTrue("Not in any navKeys { } block: $missing", missing.isEmpty())
    }

    @Test
    fun `every key comes back from a saved back stack as it went in`() {
        val keys = keyTypes.sortedBy { it.name }.map(::sampleKey)

        val encoded = serialization.json.encodeToString(stack, keys)

        assertEquals(keys, serialization.json.decodeFromString(stack, encoded))
    }

    // The navigation module's own keys (the presentation host's empty base) never reach a saved stack.
    private fun isConcreteNavKey(type: Class<*>): Boolean =
        AppNavKey::class.java.isAssignableFrom(type) && !type.isInterface && !Modifier.isAbstract(type.modifiers) &&
            !type.name.startsWith(NAVIGATION_PACKAGE)

    /** What a feature's nav module contributes: an object's no-argument function returning a module. */
    private fun navKeyModules(type: Class<*>): List<SerializersModule> {
        // Only a class that declares such a function is initialised: most objects here are not
        // meant to start up outside the app.
        val providers = runCatching { type.declaredMethods.toList() }.getOrDefault(emptyList())
            .filter { it.parameterCount == 0 && it.returnType == SerializersModule::class.java && !Modifier.isStatic(it.modifiers) }
        if (providers.isEmpty()) return emptyList()
        val instance = runCatching { type.getDeclaredField("INSTANCE").get(null) }.getOrNull() ?: return emptyList()
        return providers
            .map { it.invoke(instance) as SerializersModule }
            .filter { it.navKeyTypes().isNotEmpty() }
    }

    private fun SerializersModule.navKeyTypes(): Set<Class<*>> {
        val found = mutableSetOf<Class<*>>()
        dumpTo(
            object : SerializersModuleCollector {
                override fun <T : Any> contextual(
                    kClass: KClass<T>,
                    provider: (typeArgumentsSerializers: List<KSerializer<*>>) -> KSerializer<*>,
                ) = Unit

                override fun <Base : Any, Sub : Base> polymorphic(
                    baseClass: KClass<Base>,
                    actualClass: KClass<Sub>,
                    actualSerializer: KSerializer<Sub>,
                ) {
                    if (baseClass == AppNavKey::class) found += actualClass.java
                }

                override fun <Base : Any> polymorphicDefaultSerializer(
                    baseClass: KClass<Base>,
                    defaultSerializerProvider: (value: Base) -> SerializationStrategy<Base>?,
                ) = Unit

                override fun <Base : Any> polymorphicDefaultDeserializer(
                    baseClass: KClass<Base>,
                    defaultDeserializerProvider: (className: String?) -> DeserializationStrategy<Base>?,
                ) = Unit
            },
        )
        return found
    }

    /** A key with every field filled, so a field that does not survive the trip shows up too. */
    private fun sampleKey(type: Class<*>): AppNavKey {
        val serializer = serializer(type)
        return Json.decodeFromJsonElement(serializer, sampleOf(serializer.descriptor)) as AppNavKey
    }

    private fun sampleOf(descriptor: SerialDescriptor): JsonElement = when (descriptor.kind) {
        PrimitiveKind.STRING, PrimitiveKind.CHAR -> JsonPrimitive("k")
        PrimitiveKind.BOOLEAN -> JsonPrimitive(true)
        PrimitiveKind.BYTE, PrimitiveKind.SHORT, PrimitiveKind.INT, PrimitiveKind.LONG -> JsonPrimitive(7)
        PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> JsonPrimitive(7.5)
        SerialKind.ENUM -> JsonPrimitive(descriptor.getElementName(descriptor.elementsCount - 1))
        StructureKind.OBJECT -> JsonObject(emptyMap())
        StructureKind.LIST -> JsonArray(listOf(sampleOf(descriptor.getElementDescriptor(0))))
        StructureKind.CLASS -> JsonObject(
            (0 until descriptor.elementsCount).associate { index ->
                descriptor.getElementName(index) to sampleOf(descriptor.getElementDescriptor(index))
            },
        )
        else -> error("No sample value for ${descriptor.serialName} (${descriptor.kind})")
    }

    /** The app's own classes and every module's, without this test source set's stand-in keys. */
    private fun appClasses(): List<Class<*>> {
        val ownOutput = File(NavKeyRegistrationTest::class.java.protectionDomain.codeSource.location.toURI())
        val loader = NavKeyRegistrationTest::class.java.classLoader
        return System.getProperty("java.class.path").split(File.pathSeparator)
            .map(::File)
            .filter { it != ownOutput }
            .flatMap(::classNames)
            .filter { it.startsWith(APP_PACKAGE) }
            .mapNotNull { name -> runCatching { Class.forName(name, false, loader) }.getOrNull() }
    }

    private fun classNames(entry: File): List<String> {
        val paths = when {
            entry.isDirectory -> entry.walk().filter { it.isFile }.map { it.relativeTo(entry).invariantSeparatorsPath }.toList()
            entry.isFile && entry.extension == "jar" -> JarFile(entry).use { jar -> jar.entries().asSequence().map { it.name }.toList() }
            else -> emptyList()
        }
        return paths.filter { it.endsWith(".class") }.map { it.removeSuffix(".class").replace('/', '.') }
    }

    private companion object {
        const val APP_PACKAGE = "io.example.fitcnetdb."
        const val NAVIGATION_PACKAGE = "io.example.fitcnetdb.core.navigation."
    }
}
