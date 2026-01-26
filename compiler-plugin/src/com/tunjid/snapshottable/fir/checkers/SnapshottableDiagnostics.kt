package com.tunjid.snapshottable.fir.checkers

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor

internal object SnapshottableDiagnostics : KtDiagnosticsContainer() {
    val NOT_SNAPSHOTTABLE_INTERFACE by error0<KtNamedDeclaration>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val NOT_SNAPSHOTTABLE_SPEC by error0<KtNamedDeclaration>(SourceElementPositioningStrategies.DECLARATION_NAME)

    val NO_SNAPSHOTTABLE_SPEC by error0<KtNamedDeclaration>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val NO_SNAPSHOTTABLE_INTERFACE by error0<KtNamedDeclaration>(SourceElementPositioningStrategies.DECLARATION_NAME)

    val NO_PRIMARY_CONSTRUCTOR by error0<KtNamedDeclaration>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val PRIVATE_CONSTRUCTOR by error0<KtPrimaryConstructor>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)
    val ILLEGAL_VISIBILITY_MODIFIER by error0<KtParameter>(SourceElementPositioningStrategies.VISIBILITY_MODIFIER)

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = Renderers

    object Renderers : BaseDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("Snapshottable sandbox errors") {
            it.put(NOT_SNAPSHOTTABLE_INTERFACE, "'@Snapshottable' should be declared on an interface.")
            it.put(NOT_SNAPSHOTTABLE_SPEC, "'@Snapshottable.Spec' requires final classes.")

            it.put(NO_SNAPSHOTTABLE_SPEC, "'@Snapshottable' interface does not have an accompanying '@Snapshottable.Spec'.")
            it.put(NO_SNAPSHOTTABLE_INTERFACE, "'@Snapshottable.Spec' is not nested in discoverable '@Snapshottable' parent interface.")

            it.put(NO_PRIMARY_CONSTRUCTOR, "'@Snapshottable.Spec' requires a primary constructor.")
            it.put(PRIVATE_CONSTRUCTOR, "'@Snapshottable.Spec' requires a public constructor.")
            it.put(ILLEGAL_VISIBILITY_MODIFIER, "'@Snapshottable.Spec' cannot have non public fields in it's primary constructor.")
        }
    }
}
