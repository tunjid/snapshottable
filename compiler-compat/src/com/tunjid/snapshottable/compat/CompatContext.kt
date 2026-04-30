package com.tunjid.snapshottable.compat

import java.util.ServiceLoader
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.plugin.ClassBuildingContext
import org.jetbrains.kotlin.fir.plugin.PropertyBuildingContext
import org.jetbrains.kotlin.fir.plugin.SimpleFunctionBuildingContext
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.Name

/**
 * Service-locator-dispatched compatibility surface for Kotlin compiler APIs that change shape
 * across versions.
 *
 * A [Factory] implementation lives in each per-version sibling module (e.g. `compiler-compat:k2310`)
 * and is registered via `META-INF/services`. At runtime, [Companion.create] reads the Kotlin
 * compiler version from `META-INF/compiler.version` on the classpath and picks the factory with the
 * highest `minVersion` that is still `<=` the current compiler version.
 *
 * The surface is intentionally narrow — methods enter this interface only when an actual
 * incompatibility between supported Kotlin versions can be demonstrated.
 */
public interface CompatContext {

    // ---- Extension registration ----

    /**
     * Registers a [FirExtensionRegistrar]. The registration API moved in 2.4.x; implementations
     * adapt to whichever surface the hosting compiler exposes.
     *
     * Called from `SnapshottablePluginComponentRegistrar.registerExtensions`.
     */
    public fun CompilerPluginRegistrar.ExtensionStorage.registerFirExtensionCompat(
        extension: FirExtensionRegistrar,
    )

    /**
     * Registers an [IrGenerationExtension]. Counterpart to [registerFirExtensionCompat].
     *
     * Called from `SnapshottablePluginComponentRegistrar.registerExtensions`.
     */
    public fun CompilerPluginRegistrar.ExtensionStorage.registerIrExtensionCompat(
        extension: IrGenerationExtension,
    )

    // ---- FIR declaration-generation DSL (churn hotspot) ----

    /**
     * Wraps `org.jetbrains.kotlin.fir.plugin.createMemberFunction`. The underlying
     * `FirSimpleFunctionBuilder` was renamed to `FirNamedFunctionBuilder` in 2.3.20; the inlined
     * DSL can produce runtime linkage errors when the plugin is compiled against a different
     * builder type than the host compiler provides.
     *
     * Returns the function symbol directly — the concrete FIR function class is named
     * differently in each Kotlin version, so the symbol (whose type is stable) is the friendlier
     * return value. Callers that need to mutate the underlying declaration can do so via
     * `symbol.fir`.
     *
     * Called from `fir/Factory.kt` (`createFunSnapshotUpdate`, `createFunConversion`).
     */
    public fun FirExtension.createMemberFunctionCompat(
        owner: FirClassSymbol<*>,
        key: GeneratedDeclarationKey,
        name: Name,
        returnType: ConeKotlinType,
        config: SimpleFunctionBuildingContext.() -> Unit = {},
    ): FirNamedFunctionSymbol

    /**
     * Wraps `org.jetbrains.kotlin.fir.plugin.createMemberProperty`.
     *
     * Called from `fir/Factory.kt` (`maybeCreatePropertyOnInterfaceOrMutableClass`).
     */
    public fun FirExtension.createMemberPropertyCompat(
        owner: FirClassSymbol<*>,
        key: GeneratedDeclarationKey,
        name: Name,
        returnType: ConeKotlinType,
        isVal: Boolean = true,
        hasBackingField: Boolean = true,
        config: PropertyBuildingContext.() -> Unit = {},
    ): FirProperty

    /**
     * Wraps `org.jetbrains.kotlin.fir.plugin.createNestedClass`.
     *
     * Called from `fir/Factory.kt` (`generateMutableClass`).
     */
    public fun FirExtension.createNestedClassCompat(
        owner: FirClassSymbol<*>,
        name: Name,
        key: GeneratedDeclarationKey,
        classKind: ClassKind = ClassKind.CLASS,
        config: ClassBuildingContext.() -> Unit = {},
    ): FirRegularClass

    // ---- Defensive wrap: FirDeclarationStatus.copy ----

    /**
     * Wraps `FirDeclarationStatus.copy(...)`. The native copy signature has churned (e.g. added
     * `hasMustUseReturnValue`/`returnValueStatus` parameters) across 2.3.x. Snapshottable only
     * needs to flip `isOverride`, so the compat surface exposes just the three fields we care
     * about.
     *
     * Called from `fir/SnapshottableStatusTransformer.transformStatus`.
     */
    public fun FirDeclarationStatus.copyCompat(
        isOverride: Boolean = this.isOverride,
        visibility: Visibility? = this.visibility,
        modality: Modality? = this.modality,
    ): FirDeclarationStatus

    // ---- Factory / ServiceLoader plumbing ----

    public interface Factory {
        /** The lowest Kotlin compiler version this factory is compatible with. */
        public val minVersion: String

        /** Creates a [CompatContext] bound to this factory's compiler version. */
        public fun create(): CompatContext

        public companion object {
            private const val COMPILER_VERSION_FILE = "META-INF/compiler.version"

            /**
             * Reads `META-INF/compiler.version` from the classloader that holds
             * [FirExtensionRegistrar] (i.e. the kotlin-compiler jar's loader) and parses it as a
             * [KotlinToolingVersion], or returns null if the file is not present or is blank.
             */
            public fun loadCompilerVersionOrNull(): KotlinToolingVersion? =
                loadCompilerVersionStringOrNull()?.let(::KotlinToolingVersion)

            private fun loadCompilerVersionStringOrNull(): String? {
                val stream = FirExtensionRegistrar::class.java.classLoader
                    ?.getResourceAsStream(COMPILER_VERSION_FILE) ?: return null
                return stream.bufferedReader().use { it.readText() }.takeUnless(String::isBlank)
            }
        }
    }

    public companion object {
        /**
         * Loads all available [Factory] implementations via [ServiceLoader] and returns a
         * [CompatContext] from the factory whose `minVersion` is the highest that is still
         * `<=` [knownVersion] (or the detected compiler version, if [knownVersion] is null).
         *
         * @throws IllegalStateException if the compiler version cannot be determined or no
         *   compatible factory is available.
         */
        public fun create(knownVersion: KotlinToolingVersion? = null): CompatContext =
            resolveFactory(knownVersion).create()

        private fun loadFactories(): Sequence<Factory> =
            ServiceLoader.load(Factory::class.java, Factory::class.java.classLoader).asSequence()

        internal fun resolveFactory(
            knownVersion: KotlinToolingVersion? = null,
            factories: Sequence<Factory> = loadFactories(),
        ): Factory {
            val factoryList = factories.toList()
            val current = knownVersion
                ?: Factory.loadCompilerVersionOrNull()
                ?: error(
                    "Cannot determine Kotlin compiler version: 'META-INF/compiler.version' " +
                        "was not found on the classpath and no knownVersion was supplied",
                )
            return factoryList
                .filter { current >= KotlinToolingVersion(it.minVersion) }
                .maxByOrNull { KotlinToolingVersion(it.minVersion) }
                ?: error(
                    "No compatible snapshottable compat factory for Kotlin $current. " +
                        "Available factories: ${factoryList.joinToString { it.minVersion }}",
                )
        }
    }
}
