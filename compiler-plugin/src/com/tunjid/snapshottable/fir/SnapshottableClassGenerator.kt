package com.tunjid.snapshottable.fir

import com.tunjid.snapshottable.Snapshottable
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class SnapshottableClassGenerator(
    session: FirSession,
) : FirDeclarationGenerationExtension(session) {

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(Snapshottable.specAnnotationDeclarationPredicate)
        register(Snapshottable.annotationDeclarationPredicate)
    }

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirNamedFunctionSymbol> = with(session.filters) {
        val owner = context?.owner ?: return emptyList()
        val classId = callableId.classId ?: return emptyList()
        val function = when {
            isMutableSnapshot(classId) -> when (callableId.callableName) {
                MEMBER_FUN_NAME_UPDATE ->
                    createFunMutableMutate(
                        mutableClassSymbol = owner,
                        snapshottableClassSymbol = nestedClassIdToSpecSymbol(
                            nestedClassId = classId,
                        ) ?: return emptyList(),
                        callableId = callableId,
                    )

                else -> null
            }

            isSnapshottableInterfaceCompanion(classId) -> when (callableId.callableName) {
                COMPANION_FUN_NAME_TO_SPEC ->
                    createFunCompanionConversion(
                        companionSymbol = owner,
                        inputClassSymbol = nestedClassIdToMutableSymbol(
                            nestedClassId = owner.classId,
                        ) ?: return emptyList(),
                        outputClassSymbol = nestedClassIdToSpecSymbol(
                            nestedClassId = owner.classId,
                        ) ?: return emptyList(),
                        callableId = callableId,
                    )

                COMPANION_FUN_NAME_TO_SNAPSHOT_MUTABLE ->
                    createFunCompanionConversion(
                        companionSymbol = owner,
                        inputClassSymbol = nestedClassIdToSpecSymbol(
                            nestedClassId = owner.classId,
                        ) ?: return emptyList(),
                        outputClassSymbol = nestedClassIdToMutableSymbol(
                            nestedClassId = owner.classId,
                        ) ?: return emptyList(),
                        callableId = callableId,
                    )

                else -> null
            }

            else -> null
        }

        if (function == null) return emptyList()
        return listOf(function.symbol)
    }

    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirPropertySymbol> = with(session.filters) {
        val owner = context?.owner ?: return emptyList()
        val classId = callableId.classId ?: return emptyList()
        when {
            isSnapshottableInterface(classId) ->
                maybeCreatePropertyOnInterfaceOrMutableClass(
                    classSymbol = owner,
                    snapshottableClassSymbol = snapshottableInterfaceIdToSpecSymbol(
                        snapshottableInterfaceId = classId,
                    ) ?: return emptyList(),
                    callableId = callableId,
                )
                    ?.symbol
                    ?.let(::listOf)
                    .orEmpty()

            isMutableSnapshot(classId) ->
                maybeCreatePropertyOnInterfaceOrMutableClass(
                    classSymbol = owner,
                    snapshottableClassSymbol = nestedClassIdToSpecSymbol(
                        nestedClassId = classId,
                    ) ?: return emptyList(),
                    callableId = callableId,
                )
                    ?.symbol
                    ?.let(::listOf)
                    .orEmpty()

            else -> emptyList()
        }
    }

    override fun generateConstructors(
        context: MemberGenerationContext,
    ): List<FirConstructorSymbol> = with(session.filters) {
        val ownerClassId = context.owner.classId
        val constructor = when {
            isMutableSnapshot(ownerClassId) -> createConstructor(
                owner = context.owner,
                key = Snapshottable.Key,
                isPrimary = true,
            ) {
                val parameters = nestedClassIdToSnapshottableInterfaceClassId(
                    nestedClassId = context.owner.classId,
                )
                    ?.let(::snapshottableSourceParameterSymbols)
                    ?: return@createConstructor

                parameters.forEach { parameter ->
                    valueParameter(
                        name = parameter.name,
                        type = parameter.resolvedReturnType,
                        hasDefaultValue = false,
                        key = Snapshottable.Key,
                    )
                }
            }

            isSnapshottableInterfaceCompanion(ownerClassId) -> createDefaultPrivateConstructor(
                owner = context.owner,
                key = Snapshottable.Key,
            )

            else -> error("Can't generate constructor for ${ownerClassId.asSingleFqName()}")
        }
        return listOf(constructor.symbol)
    }

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext,
    ): Set<Name> = with(session.filters) {
        val classId = classSymbol.classId
        when {
            isSnapshottableInterface(classId) ->
                snapshottableSourceParameterNames(snapshottableParentId = classId)
                    .toSet()

            isSnapshottableInterfaceCompanion(classId) ->
                setOf(
                    SpecialNames.INIT,
                    COMPANION_FUN_NAME_TO_SNAPSHOT_MUTABLE,
                    COMPANION_FUN_NAME_TO_SPEC,
                )

            isMutableSnapshot(classId) ->
                buildSet {
                    add(SpecialNames.INIT)
                    addAll(
                        elements = nestedClassIdToSnapshottableInterfaceClassId(nestedClassId = classId)
                            ?.let(::snapshottableSourceParameterNames)
                            .orEmpty(),
                    )
                    add(MEMBER_FUN_NAME_UPDATE)
                }

            else -> emptySet()
        }
    }

    override fun getNestedClassifiersNames(
        classSymbol: FirClassSymbol<*>,
        context: NestedClassGenerationContext,
    ): Set<Name> = when {
        session.filters.isSnapshottableInterface(classSymbol.classId) ->
            setOf(
                CLASS_NAME_SNAPSHOT_MUTABLE,
                SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT,
            )

        else ->
            emptySet()
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? = with(session.filters) {
        if (owner !is FirRegularClassSymbol) return null
        if (!isSnapshottableInterface(owner.classId)) return null

        return when (name) {
            CLASS_NAME_SNAPSHOT_MUTABLE -> generateMutableClass(
                parentInterfaceSymbol = owner,
                mutableClassSymbol = owner,
                snapshottableClassSymbol = snapshottableInterfaceIdToSpecSymbol(
                    snapshottableInterfaceId = owner.classId,
                ) ?: return@with null,
            ).symbol

            SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT -> generateCompanionDeclaration(owner)
            else -> error("Can't generate class ${owner.classId.createNestedClassId(name).asSingleFqName()}")
        }
    }

    private fun generateCompanionDeclaration(owner: FirRegularClassSymbol): FirRegularClassSymbol? {
        if (owner.resolvedCompanionObjectSymbol != null) return null
        val companion = createCompanionObject(owner, Snapshottable.Key)
        return companion.symbol
    }

    private fun snapshottableSourceParameterSymbols(
        snapshottableParentId: ClassId,
    ): List<FirValueParameterSymbol> {
        val sourceClassSymbol = session.filters.snapshottableInterfaceIdToSpecSymbol(
            snapshottableInterfaceId = snapshottableParentId,
        ) ?: return emptyList()
        val parameters = session.getPrimaryConstructorValueParameters(sourceClassSymbol)
        return parameters
    }

    private fun snapshottableSourceParameterNames(
        snapshottableParentId: ClassId,
    ): List<Name> =
        snapshottableSourceParameterSymbols(snapshottableParentId)
            .map(FirValueParameterSymbol::name)
}
