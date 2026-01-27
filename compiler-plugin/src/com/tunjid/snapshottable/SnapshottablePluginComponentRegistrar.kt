package com.tunjid.snapshottable

import com.tunjid.snapshottable.ir.SnapshottableIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

class SnapshottablePluginComponentRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String
        get() = BuildConfig.KOTLIN_PLUGIN_ID
    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(SnapshottablePluginRegistrar())
        IrGenerationExtension.registerExtension(SnapshottableIrGenerationExtension())
    }
}
