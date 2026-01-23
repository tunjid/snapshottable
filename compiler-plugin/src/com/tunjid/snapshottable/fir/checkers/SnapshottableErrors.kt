package com.tunjid.snapshottable.fir.checkers

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtPrimaryConstructor

internal object SnapshottableErrors : KtDiagnosticsContainer() {
    val NO_PRIMARY_CONSTRUCTOR by error0<KtNamedDeclaration>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val PRIVATE_CONSTRUCTOR by error0<KtPrimaryConstructor>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)
    val NOT_SNAPSHOTTABLE_INTERFACE by error0<KtNamedDeclaration>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val NOT_SNAPSHOTTABLE_SPEC by error0<KtNamedDeclaration>(SourceElementPositioningStrategies.DECLARATION_NAME)

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = Renderers

    object Renderers : BaseDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("Snapshottable sandbox errors") {
            it.put(NOT_SNAPSHOTTABLE_INTERFACE, "'@Snapshottable' should be declared on an interface.")
            it.put(NO_PRIMARY_CONSTRUCTOR, "'@Snapshottable.Spec' requires a primary constructor.")
            it.put(PRIVATE_CONSTRUCTOR, "'@Snapshottable.Spec' requires a public constructor.")
            it.put(NOT_SNAPSHOTTABLE_SPEC, "'@Snapshottable.Spec' requires final classes")
        }
    }
}
