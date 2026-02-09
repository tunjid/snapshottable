package com.tunjid.snapshottable.fir

import com.tunjid.snapshottable.Snapshottable
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.mapToSetOrEmpty

class SnapshottableFilters(
    session: FirSession,
) : FirExtensionSessionComponent(session) {

    // Symbols for interfaces which have Snapshottable annotation.
    private val snapshottableParentInterfaces by lazy {
        session.predicateBasedProvider.getSymbolsByPredicate(Snapshottable.annotationLookupPredicate)
            .filterIsInstance<FirRegularClassSymbol>()
            .filter(FirRegularClassSymbol::isInterface)
            .toSet()
    }

    // IDs for nested Spec classes.
    private val snapshotSpecClasses by lazy {
        session.predicateBasedProvider.getSymbolsByPredicate(Snapshottable.specAnnotationLookupPredicate)
            .filterIsInstance<FirRegularClassSymbol>()
            .filterNot(FirRegularClassSymbol::isInterface)
            .toSet()
    }

    // IDs for nested Mutable classes.
    private val mutableSnapshotClassIds by lazy {
        snapshottableParentInterfaces.mapToSetOrEmpty { it.classId.mutable }
    }

    fun isSnapshottableInterface(
        symbol: FirClassSymbol<*>,
    ): Boolean = snapshottableParentInterfaces.contains(symbol)

    fun isSnapshottableInterfaceCompanion(
        symbol: FirClassSymbol<*>,
    ) = snapshottableParentInterfaces.any { it.classId.companion == symbol.classId }

    fun isSnapshotSpec(
        symbol: FirClassSymbol<*>,
    ) = snapshotSpecClasses.contains(symbol) && nestedClassSymbolToSnapshottableInterfaceClassSymbol(symbol) != null

    fun isSnapshotMutable(
        symbol: FirClassSymbol<*>,
    ) = mutableSnapshotClassIds.contains(symbol.classId)

    fun nestedClassSymbolToSnapshottableInterfaceClassSymbol(
        nestedClassSymbol: FirClassSymbol<*>,
    ): FirClassSymbol<*>? = generateSequence(
        seed = nestedClassSymbol.classId,
        nextFunction = ClassId::outerClassId,
    )
        .firstNotNullOfOrNull { classId ->
            snapshottableParentInterfaces.singleOrNull { it.classId == classId }
        }

    fun snapshottableInterfaceSymbolToSpecSymbol(
        snapshottableInterfaceSymbol: FirClassSymbol<*>,
    ): FirRegularClassSymbol? = snapshotSpecClasses.singleOrNull {
        it.classId.parentClassId == snapshottableInterfaceSymbol.classId
    }

    fun nestedClassSymbolToSpecSymbol(
        nestedClassSymbol: FirClassSymbol<*>,
    ): FirRegularClassSymbol? =
        nestedClassSymbolToSnapshottableInterfaceClassSymbol(nestedClassSymbol = nestedClassSymbol)
            ?.let(::snapshottableInterfaceSymbolToSpecSymbol)

    fun nestedClassSymbolToMutableSymbol(
        nestedClassSymbol: FirClassSymbol<*>,
    ): FirClassSymbol<*>? =
        nestedClassSymbolToSnapshottableInterfaceClassSymbol(nestedClassSymbol = nestedClassSymbol)
            ?.classId
            ?.mutable
            ?.let(session::findClassSymbol)

    fun specPrimaryConstructor(
        specSymbol: FirClassSymbol<*>,
    ): FirConstructorSymbol? {
        val scope = specSymbol.declaredMemberScope(
            session = session,
            memberRequiredPhase = FirResolvePhase.RAW_FIR,
        )

        return scope.getDeclaredConstructors()
            .firstOrNull(FirConstructorSymbol::isPrimary)
    }
}

internal val FirSession.filters: SnapshottableFilters by FirSession.sessionComponentAccessor()
