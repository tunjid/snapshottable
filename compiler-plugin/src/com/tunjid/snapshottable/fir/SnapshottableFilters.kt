package com.tunjid.snapshottable.fir

import com.tunjid.snapshottable.Snapshottable
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.mapToSetOrEmpty

class SnapshottableFilters(
    session: FirSession,
) : FirExtensionSessionComponent(session) {

    // Symbols for interfaces which have Snapshottable annotation.
    private val snapshottableParentInterfaces by lazy {
        session.predicateBasedProvider.getSymbolsByPredicate(Snapshottable.annotationLookupPredicate)
            .filterIsInstance<FirRegularClassSymbol>()
            // TODO: This check should be more rigid
            .filter(FirRegularClassSymbol::isInterface)
            .toSet()
    }

    private val snapshottableParentInterfaceIdsToSnapshottableSpecSymbols by lazy {
        session.predicateBasedProvider.getSymbolsByPredicate(Snapshottable.specAnnotationLookupPredicate)
            .filterIsInstance<FirRegularClassSymbol>()
            .mapNotNull { symbol ->
                symbol.resolvedSuperTypes.firstNotNullOfOrNull { superType ->
                    val parentInterfaceId = superType.classId ?: return@mapNotNull null
                    if (parentInterfaceId !in snapshottableInterfaceIds) return@mapNotNull null
                    parentInterfaceId to symbol
                }
            }
            .toMap()
    }

    val snapshottableInterfaceIds by lazy {
        snapshottableParentInterfaces.mapToSetOrEmpty { it.classId }
    }

    // IDs for Snapshottable-annotated classes' companion objects.
    val snapshottableCompanionClassIds by lazy {
        snapshottableParentInterfaces.mapToSetOrEmpty { it.classId.companion }
    }

    // IDs for nested Mutable classes.
    val mutableSnapshotClassIds by lazy {
        snapshottableParentInterfaces.mapToSetOrEmpty { it.classId.mutable }
    }

    // IDs for nested Spec classes.
    private val snapshotSpecClassIds by lazy {
        snapshottableParentInterfaceIdsToSnapshottableSpecSymbols.values
            .mapToSetOrEmpty(FirRegularClassSymbol::classId)
    }

    fun isSnapshottableSpec(
        classId: ClassId
    ) = snapshotSpecClassIds.contains(classId)

    fun mutableClassIdToSnapshottableInterfaceSymbol(
        mutableClassId: ClassId
    ): ClassId = requireNotNull(
        value = generateSequence(
            seed = mutableClassId,
            nextFunction = ClassId::outerClassId
        )
            .firstOrNull(snapshottableInterfaceIds::contains),
        lazyMessage = {
            "Unable to resolve parent sealed interface for $mutableClassId"
        }
    )

    fun snapshottableInterfaceIdToSpecSymbol(
        snapshottableInterfaceId: ClassId
    ): FirRegularClassSymbol = requireNotNull(
        value = snapshottableParentInterfaceIdsToSnapshottableSpecSymbols[snapshottableInterfaceId],
        lazyMessage = {
            "Unable to resolve source snapshottable class under $snapshottableInterfaceId"
        }
    )

    fun mutableClassIdToSpecSymbol(
        mutableClassId: ClassId
    ): FirRegularClassSymbol = snapshottableInterfaceIdToSpecSymbol(
        snapshottableInterfaceId = mutableClassIdToSnapshottableInterfaceSymbol(
            mutableClassId = mutableClassId,
        )
    )
}

internal val FirSession.filters: SnapshottableFilters by FirSession.sessionComponentAccessor()
