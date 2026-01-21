package com.tunjid.snapshottable

import com.tunjid.snapshottable.fir.SnapshottableClassGenerator
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class SnapshottablePluginRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::SnapshottableClassGenerator
    }
}
