package com.tunjid.snapshottable

import com.tunjid.snapshottable.fir.SnapshottableClassGenerator
import com.tunjid.snapshottable.fir.SnapshottableFilters
import com.tunjid.snapshottable.fir.SnapshottableStatusTransformer
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class SnapshottablePluginRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::SnapshottableFilters
        +::SnapshottableClassGenerator
        +::SnapshottableStatusTransformer
    }
}
