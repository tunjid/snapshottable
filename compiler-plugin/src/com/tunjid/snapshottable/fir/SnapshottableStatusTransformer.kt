package com.tunjid.snapshottable.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.resolve.getContainingClass
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol

class SnapshottableStatusTransformer(
    session: FirSession,
) : FirStatusTransformerExtension(session) {

    override fun needTransformStatus(
        declaration: FirDeclaration,
    ): Boolean {
        if (declaration !is FirProperty) return false

        val containingClass = declaration.getContainingClass() ?: return false

        return session.filters.isSnapshottableSpec(containingClass.symbol.classId)
    }

    override fun transformStatus(
        status: FirDeclarationStatus,
        property: FirProperty,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean,
    ): FirDeclarationStatus = status.copy(isOverride = true)
}
