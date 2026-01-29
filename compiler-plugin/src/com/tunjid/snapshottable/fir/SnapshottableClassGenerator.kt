package com.tunjid.snapshottable.fir

import com.tunjid.snapshottable.Snapshottable
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
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
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.mapToSetOrEmpty

class SnapshottableClassGenerator(
    session: FirSession,
) : FirDeclarationGenerationExtension(session) {

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(Snapshottable.specAnnotationDeclarationPredicate)
        register(Snapshottable.annotationDeclarationPredicate)
    }

    override fun getNestedClassifiersNames(
        classSymbol: FirClassSymbol<*>,
        context: NestedClassGenerationContext,
    ): Set<Name> = with(session.filters) {
        when {
            isSnapshottableInterface(classSymbol) ->
                setOf(
                    CLASS_NAME_SNAPSHOT_MUTABLE,
                    SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT,
                )

            else ->
                emptySet()
        }
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? = with(session.filters) {
        if (owner !is FirRegularClassSymbol) return null
        if (!isSnapshottableInterface(owner)) return null

        return when (name) {
            CLASS_NAME_SNAPSHOT_MUTABLE -> generateMutableClass(
                parentInterfaceSymbol = owner,
            )

            SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT -> generateCompanionDeclaration(
                parentInterfaceSymbol = owner,
            )
            else -> error("Can't generate class ${owner.classId.createNestedClassId(name).asSingleFqName()}")
        }
    }

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext,
    ): Set<Name> = with(session.filters) {
        when {
            isSnapshottableInterface(classSymbol) ->
                snapshottableInterfaceSymbolToSpecSymbol(classSymbol)
                    ?.let(::specPrimaryConstructor)
                    ?.valueParameterSymbols
                    ?.mapToSetOrEmpty(FirValueParameterSymbol::name)
                    .orEmpty()

            isSnapshottableInterfaceCompanion(classSymbol) ->
                setOf(
                    SpecialNames.INIT,
                    COMPANION_FUN_NAME_TO_SNAPSHOT_MUTABLE,
                    COMPANION_FUN_NAME_TO_SPEC,
                )

            isMutableSnapshot(classSymbol) ->
                buildSet {
                    add(SpecialNames.INIT)
                    addAll(
                        context.owner
                            .requireKey<Snapshottable.Keys.SnapshotMutable>()
                            .specPrimaryConstructor
                            .valueParameterSymbols
                            .map(FirValueParameterSymbol::name),
                    )
                    add(MEMBER_FUN_NAME_UPDATE)
                }

            else -> emptySet()
        }
    }

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirNamedFunctionSymbol> = with(session.filters) {
        val owner = context?.owner ?: return emptyList()
        val function = when {
            isMutableSnapshot(owner) -> when (callableId.callableName) {
                MEMBER_FUN_NAME_UPDATE ->
                    createFunSnapshotUpdate(
                        mutableClassSymbol = owner,
                        callableId = callableId,
                    )

                else -> null
            }

            isSnapshottableInterfaceCompanion(owner) -> when (callableId.callableName) {
                COMPANION_FUN_NAME_TO_SPEC ->
                    createFunCompanionConversion(
                        companionSymbol = owner,
                        inputClassSymbol = nestedClassSymbolToMutableSymbol(
                            nestedClassSymbol = owner,
                        ) ?: return emptyList(),
                        outputClassSymbol = nestedClassSymbolToSpecSymbol(
                            nestedClassSymbol = owner,
                        ) ?: return emptyList(),
                        callableId = callableId,
                    )

                COMPANION_FUN_NAME_TO_SNAPSHOT_MUTABLE ->
                    createFunCompanionConversion(
                        companionSymbol = owner,
                        inputClassSymbol = nestedClassSymbolToSpecSymbol(
                            nestedClassSymbol = owner,
                        ) ?: return emptyList(),
                        outputClassSymbol = nestedClassSymbolToMutableSymbol(
                            nestedClassSymbol = owner,
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
        when {
            isSnapshottableInterface(owner) ->
                maybeCreatePropertyOnInterfaceOrMutableClass(
                    classSymbol = owner,
                    specSymbol = snapshottableInterfaceSymbolToSpecSymbol(
                        snapshottableInterfaceSymbol = owner,
                    ) ?: return emptyList(),
                    callableId = callableId,
                )
                    ?.symbol
                    ?.let(::listOf)
                    .orEmpty()

            isMutableSnapshot(owner) ->
                maybeCreatePropertyOnInterfaceOrMutableClass(
                    classSymbol = owner,
                    specSymbol = nestedClassSymbolToSpecSymbol(
                        nestedClassSymbol = owner,
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
    ): List<FirConstructorSymbol> = with(session.filters) filters@{
        val constructor = when {
            isMutableSnapshot(context.owner) -> createConstructor(
                owner = context.owner,
                key = context.owner.requireKey(),
                isPrimary = true,
            ) {
                val constructor = context.owner
                    .requireKey<Snapshottable.Keys.SnapshotMutable>()
                    .specPrimaryConstructor

                constructor.valueParameterSymbols.forEach { parameter ->
                    valueParameter(
                        name = parameter.name,
                        type = parameter.resolvedReturnType,
                        hasDefaultValue = false,
                    )
                }
            }

            isSnapshottableInterfaceCompanion(context.owner) -> createDefaultPrivateConstructor(
                owner = context.owner,
                key = context.owner.requireKey(),
            )

            else -> error("Can't generate constructor for ${context.owner.classId.asSingleFqName()}")
        }
        return listOf(constructor.symbol)
    }
}
