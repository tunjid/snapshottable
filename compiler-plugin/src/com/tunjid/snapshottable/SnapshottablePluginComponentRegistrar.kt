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
        val compatContext = try {
            CompatContext.create()
        } catch (t: Throwable) {
            System.err.println("[snapshottable] Skipping: no compatible CompatContext factory")
            t.printStackTrace()
            return
        }
        with(compatContext) {
            registerFirExtensionCompat(SnapshottablePluginRegistrar(compatContext))
            registerIrExtensionCompat(SnapshottableIrGenerationExtension())
        }
    }
}
