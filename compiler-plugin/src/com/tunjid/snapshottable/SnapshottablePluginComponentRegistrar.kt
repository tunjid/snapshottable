package com.tunjid.snapshottable

import com.tunjid.snapshottable.compat.CompatContext
import com.tunjid.snapshottable.ir.SnapshottableIrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration

class SnapshottablePluginComponentRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String
        get() = BuildConfig.KOTLIN_PLUGIN_ID
    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        // Use the alias-aware path: IDE-bundled Kotlin compilers (IntelliJ, Android Studio) often
        // advertise version strings that don't match any released Kotlin tag. CompatContext.
        // createForRuntime translates those to the underlying real version before factory
        // selection, and returns null when the IDE build is marked CLI_ONLY in ide-mappings.txt.
        val compatContext = try {
            CompatContext.createForRuntime()
        } catch (t: Throwable) {
            System.err.println("[snapshottable] Skipping: failed to resolve a CompatContext factory")
            t.printStackTrace()
            return
        }
        if (compatContext == null) {
            System.err.println(
                "[snapshottable] Skipping: detected Kotlin compiler version is not supported in this IDE build",
            )
            return
        }
        with(compatContext) {
            registerFirExtensionCompat(
                SnapshottablePluginRegistrar(
                    compatContext = compatContext,
                ),
            )
            registerIrExtensionCompat(
                SnapshottableIrGenerationExtension(),
            )
        }
    }
}
