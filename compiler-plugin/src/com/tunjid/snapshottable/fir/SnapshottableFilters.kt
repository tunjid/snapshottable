package com.tunjid.snapshottable.fir

import com.tunjid.snapshottable.Snapshottable
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
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

    private val snapshottableInterfaceIds by lazy {
        snapshottableParentInterfaces.mapToSetOrEmpty { it.classId }
    }

    // IDs for Snapshottable-annotated classes' companion objects.
    private val snapshottableCompanionClassIds by lazy {
        snapshottableParentInterfaces.mapToSetOrEmpty { it.classId.companion }
    }

    // IDs for nested Mutable classes.
    private val mutableSnapshotClassIds by lazy {
        snapshottableParentInterfaces.mapToSetOrEmpty { it.classId.mutable }
    }

    // IDs for nested Spec classes.
    private val snapshotSpecClassIds by lazy {
        snapshottableParentInterfaceIdsToSnapshottableSpecSymbols.values
            .mapToSetOrEmpty(FirRegularClassSymbol::classId)
    }

    fun isSnapshottableInterface(
        classId: ClassId
    ) = snapshottableInterfaceIds.contains(classId)

    fun isSnapshottableInterfaceCompanion(
        classId: ClassId
    ) = snapshottableCompanionClassIds.contains(classId)

    fun isSnapshottableSpec(
        classId: ClassId
    ) = snapshotSpecClassIds.contains(classId)

    fun isMutableSnapshot(
        classId: ClassId
    ) = mutableSnapshotClassIds.contains(classId)

    fun nestedClassIdToSnapshottableInterfaceClassId(
        nestedClassId: ClassId
    ): ClassId = requireNotNull(
        value = generateSequence(
            seed = nestedClassId,
            nextFunction = ClassId::outerClassId
        )
            .firstOrNull(::isSnapshottableInterface),
        lazyMessage = {
            "Unable to resolve parent sealed interface for $nestedClassId"
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

    fun nestedClassIdToSpecSymbol(
        nestedClassId: ClassId
    ): FirRegularClassSymbol = snapshottableInterfaceIdToSpecSymbol(
        snapshottableInterfaceId = nestedClassIdToSnapshottableInterfaceClassId(
            nestedClassId = nestedClassId,
        )
    )

    fun nestedClassIdToMutableSymbol(
        nestedClassId: ClassId
    ): FirClassSymbol<*> = requireNotNull(
        session.findClassSymbol(
            nestedClassIdToSnapshottableInterfaceClassId(
                nestedClassId = nestedClassId,
            ).mutable
        )
    )
}

internal val FirSession.filters: SnapshottableFilters by FirSession.sessionComponentAccessor()
