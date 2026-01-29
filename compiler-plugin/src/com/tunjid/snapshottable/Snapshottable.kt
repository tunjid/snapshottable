package com.tunjid.snapshottable

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate.BuilderContext.annotated
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.GeneratedByPlugin
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

object Snapshottable {
    private const val SPEC_ANNOTATION = "com.tunjid.snapshottable.SnapshotSpec"
    private const val ANNOTATION = "com.tunjid.snapshottable.Snapshottable"

    private val specAnnotationFullyQualifiedName = FqName(SPEC_ANNOTATION)
    val specAnnotationLookupPredicate = annotated(specAnnotationFullyQualifiedName)
    val specAnnotationDeclarationPredicate = DeclarationPredicate.create {
        annotated(FqName(SPEC_ANNOTATION))
    }

    private val annotationFullyQualifiedName = FqName(ANNOTATION)
    val annotationLookupPredicate = annotated(annotationFullyQualifiedName)
    val annotationDeclarationPredicate = DeclarationPredicate.create {
        annotated(FqName(ANNOTATION))
    }

    sealed class Keys : GeneratedDeclarationKey() {
        data class Companion(
            val parentInterfaceClassId: ClassId,
            val specPrimaryConstructor: FirConstructorSymbol,
        ) : Keys()

        data class SnapshotMutable(
            val specPrimaryConstructor: FirConstructorSymbol,
        ) : Keys()

        data object Default : Keys()
    }

    val ORIGIN = GeneratedByPlugin(Keys.Default)
}
