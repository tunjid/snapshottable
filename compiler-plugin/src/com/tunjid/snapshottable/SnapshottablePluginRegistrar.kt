package com.tunjid.snapshottable

import com.tunjid.snapshottable.compat.CompatContext
import com.tunjid.snapshottable.fir.SnapshottableClassGenerator
import com.tunjid.snapshottable.fir.SnapshottableFilters
import com.tunjid.snapshottable.fir.SnapshottableStatusTransformer
import com.tunjid.snapshottable.fir.checkers.SnapshottableAdditionalCheckersExtension
import com.tunjid.snapshottable.fir.checkers.SnapshottableDiagnostics
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class SnapshottablePluginRegistrar(
    private val compatContext: CompatContext,
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::SnapshottableFilters
        +{ session: FirSession -> SnapshottableClassGenerator(session, compatContext) }
        +{ session: FirSession -> SnapshottableStatusTransformer(session, compatContext) }
        +::SnapshottableAdditionalCheckersExtension

        registerDiagnosticContainers(SnapshottableDiagnostics)
    }
}
