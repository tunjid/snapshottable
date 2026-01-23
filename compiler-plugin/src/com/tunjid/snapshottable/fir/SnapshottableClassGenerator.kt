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
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class SnapshottableClassGenerator(
    session: FirSession
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
                        snapshottableClassSymbol = session.filters.nestedClassIdToSpecSymbol(
                            nestedClassId = classId,
                        ),
                        callableId = callableId,
                    )

                else -> null
            }

            isSnapshottableInterfaceCompanion(classId) -> when (callableId.callableName) {
                COMPANION_FUN_NAME_TO_SPEC ->
                    createFunCompanionConversion(
                        companionSymbol = owner,
                        inputClassSymbol = session.filters.nestedClassIdToMutableSymbol(
                            nestedClassId = owner.classId,
                        ),
                        outputClassSymbol = session.filters.nestedClassIdToSpecSymbol(
                            nestedClassId = owner.classId,
                        ),
                        callableId = callableId,
                    )

                COMPANION_FUN_NAME_TO_SNAPSHOT_MUTABLE ->
                    createFunCompanionConversion(
                        companionSymbol = owner,
                        inputClassSymbol = session.filters.nestedClassIdToSpecSymbol(
                            nestedClassId = owner.classId,
                        ),
                        outputClassSymbol = session.filters.nestedClassIdToMutableSymbol(
                            nestedClassId = owner.classId,
                        ),
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
        context: MemberGenerationContext?
    ): List<FirPropertySymbol> {
        val owner = context?.owner ?: return emptyList()
        val classId = callableId.classId ?: return emptyList()
        return when {
            session.filters.isSnapshottableInterface(classId) ->
                createInterfaceOrMutableProperty(
                    classSymbol = owner,
                    snapshottableClassSymbol = session.filters.snapshottableInterfaceIdToSpecSymbol(
                        snapshottableInterfaceId = classId,
                    ),
                    callableId = callableId,
                )
                    ?.symbol
                    ?.let(::listOf)
                    .orEmpty()

            session.filters.isMutableSnapshot(classId) ->
                createInterfaceOrMutableProperty(
                    classSymbol = owner,
                    snapshottableClassSymbol = session.filters.nestedClassIdToSpecSymbol(
                        nestedClassId = classId,
                    ),
                    callableId = callableId,
                )
                    ?.symbol
                    ?.let(::listOf)
                    .orEmpty()

            else -> emptyList()
        }
    }

    override fun generateConstructors(
        context: MemberGenerationContext
    ): List<FirConstructorSymbol> = with(session.filters) {
        val ownerClassId = context.owner.classId
        val constructor = when {
            isMutableSnapshot(ownerClassId) -> createConstructor(
                owner = context.owner,
                key = Snapshottable.Key,
                isPrimary = true
            ) {
                val parameters = snapshottableSourceParameterSymbols(
                    snapshottableParentId = session.filters.nestedClassIdToSnapshottableInterfaceClassId(
                        nestedClassId = context.owner.classId,
                    )
                )
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
                key = Snapshottable.Key
            )

            else -> error("Can't generate constructor for ${ownerClassId.asSingleFqName()}")
        }
        return listOf(constructor.symbol)
    }

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext
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
                        snapshottableSourceParameterNames(
                            snapshottableParentId = session.filters.nestedClassIdToSnapshottableInterfaceClassId(
                                nestedClassId = classId,
                            )
                        )
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
        context: NestedClassGenerationContext
    ): FirClassLikeSymbol<*>? {
        if (owner !is FirRegularClassSymbol) return null
        if (!session.filters.isSnapshottableInterface(owner.classId)) return null

        return when (name) {
            CLASS_NAME_SNAPSHOT_MUTABLE -> generateMutableClass(
                parentInterfaceSymbol = owner,
                mutableClassSymbol = owner,
                snapshottableClassSymbol = session.filters.snapshottableInterfaceIdToSpecSymbol(
                    snapshottableInterfaceId = owner.classId
                ),
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
        snapshottableParentId: ClassId
    ): List<FirValueParameterSymbol> {
        val sourceClassSymbol = session.filters.snapshottableInterfaceIdToSpecSymbol(
            snapshottableInterfaceId = snapshottableParentId,
        )
        val parameters = session.getPrimaryConstructorValueParameters(sourceClassSymbol)
        return parameters
    }

    private fun snapshottableSourceParameterNames(
        snapshottableParentId: ClassId
    ): List<Name> =
        snapshottableSourceParameterSymbols(snapshottableParentId)
            .map(FirValueParameterSymbol::name)
}
