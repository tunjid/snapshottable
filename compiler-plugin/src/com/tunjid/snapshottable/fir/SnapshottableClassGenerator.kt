package com.tunjid.snapshottable.fir

import com.tunjid.snapshottable.Snapshottable
import com.tunjid.snapshottable.Snapshottable.snapshottableSourceSymbol
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class SnapshottableClassGenerator(
    session: FirSession
) : FirDeclarationGenerationExtension(session) {

    // Symbols for classes which have Snapshottable annotation.
    private val snapshottableClasses by lazy {
        session.predicateBasedProvider.getSymbolsByPredicate(Snapshottable.ANNOTATION_PREDICATE)
            .filterIsInstance<FirRegularClassSymbol>()
            .filter { symbol ->
                val sourceClassSymbol = session.snapshottableSourceSymbol(symbol)
                    ?: return@filter false

                if (sourceClassSymbol !is FirRegularClassSymbol) return@filter false

                val sourcePrimaryCtor = sourceClassSymbol.primaryConstructorIfAny(session) ?: return@filter false
                sourcePrimaryCtor.rawStatus.visibility != Visibilities.Private
            }
            .toSet()
    }

    private val snapshottableClassIds by lazy {
        snapshottableClasses.map { it.classId }.toSet()
    }

    // IDs for Snapshottable-annotated classes' companion objects.
    private val snapshottableCompanionClassIds by lazy {
        snapshottableClasses.map { it.classId.companion }.toSet()
    }

    // IDs for nested Mutable classes.
    private val mutableSnapshotClassIds by lazy {
        snapshottableClasses.map { it.classId.mutable }
            .toSet()
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(Snapshottable.SNAPSHOTTABLE_PREDICATE)
        register(Snapshottable.HAS_SNAPSHOTTABLE_PREDICATE)
    }


    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirNamedFunctionSymbol> {
        val owner = context?.owner ?: return emptyList()

        val function = when (callableId.classId) {
            in mutableSnapshotClassIds -> when (callableId.callableName) {
                // TODO generate other functions
                else -> createFunMutableSetter(
                    mutableClassSymbol = owner,
                    callableId = callableId,
                )
            }

            else -> null
        }

        if (function == null) return emptyList()
        return listOf(function.symbol)
    }

    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirPropertySymbol> {
        val owner = context?.owner ?: return emptyList()

        if (callableId.classId in mutableSnapshotClassIds) {
            val property = createPropertyMutableValue(
                mutableClassSymbol = owner,
                callableId = callableId,
            ) ?: return emptyList()

            return listOf(property.symbol)
        }

        return emptyList()
    }

    override fun generateConstructors(
        context: MemberGenerationContext
    ): List<FirConstructorSymbol> {
        val constructor = when (val ownerClassId = context.owner.classId) {
            in mutableSnapshotClassIds -> createConstructor(
                owner = context.owner,
                key = Snapshottable.Key,
                isPrimary = true
            )

            in snapshottableCompanionClassIds -> createDefaultPrivateConstructor(
                owner = context.owner,
                key = Snapshottable.Key
            )

            else -> error("Can't generate constructor for ${ownerClassId.asSingleFqName()}")
        }
        return listOf(constructor.symbol)
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        return when (val classId = classSymbol.classId) {
            in snapshottableClassIds -> {
                emptySet()
            }

            in snapshottableCompanionClassIds -> {
                setOf(
                    SpecialNames.INIT
                )
            }

            in mutableSnapshotClassIds -> {
                val snapshottableClassId = classId.outerClassId!!
                val sourceClassSymbol = requireNotNull(
                    session.snapshottableSourceSymbol(
                        snapshottableSymbol = session.findClassSymbol(snapshottableClassId)!!
                    )
                )
                val parameters = session.getPrimaryConstructorValueParameters(sourceClassSymbol)
                buildSet {
                    add(SpecialNames.INIT)
                    addAll(parameters.map { it.name })
                }
            }

            else -> emptySet()
        }
    }

    override fun getNestedClassifiersNames(
        classSymbol: FirClassSymbol<*>,
        context: NestedClassGenerationContext,
    ): Set<Name> {
        return when (classSymbol) {
            in snapshottableClasses -> setOf(
                MUTABLE_CLASS_NAME,
                SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT,
            )

            else -> emptySet()
        }
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext
    ): FirClassLikeSymbol<*>? {
        if (owner !is FirRegularClassSymbol) return null
        if (owner !in snapshottableClasses) return null
        return when (name) {
            MUTABLE_CLASS_NAME -> generateMutableClass(owner).symbol
            SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT -> generateCompanionDeclaration(owner)
            else -> error("Can't generate class ${owner.classId.createNestedClassId(name).asSingleFqName()}")
        }
    }

    private fun generateCompanionDeclaration(owner: FirRegularClassSymbol): FirRegularClassSymbol? {
        if (owner.resolvedCompanionObjectSymbol != null) return null
        val companion = createCompanionObject(owner, Snapshottable.Key)
        return companion.symbol
    }
}
