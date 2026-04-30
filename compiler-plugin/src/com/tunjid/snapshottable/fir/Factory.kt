@file:OptIn(org.jetbrains.kotlin.fir.symbols.SymbolInternals::class)

package com.tunjid.snapshottable.fir

import com.tunjid.snapshottable.Snapshottable
import com.tunjid.snapshottable.compat.CompatContext
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.declaredProperties
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.buildResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.ir.util.kotlinPackageFqn
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.ConstantValueKind

val CLASS_NAME_SNAPSHOT_MUTABLE = Name.identifier("SnapshotMutable")
val MEMBER_FUN_NAME_UPDATE = Name.identifier("update")
val FUN_NAME_TO_SPEC = Name.identifier("toSnapshotSpec")
val FUN_NAME_TO_SNAPSHOT_MUTABLE = Name.identifier("toSnapshotMutable")

val ClassId.mutable: ClassId get() = createNestedClassId(CLASS_NAME_SNAPSHOT_MUTABLE)
val ClassId.companion: ClassId get() = createNestedClassId(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)

inline fun <reified T : Snapshottable.Keys> FirClassSymbol<*>.requireKey(): T {
    val plugin = origin as? FirDeclarationOrigin.Plugin
        ?: throw IllegalArgumentException("key cannot be fetched. Expected FirDeclarationOrigin.Plugin, was $origin")

    val key = plugin.key

    check(key is T) {
        "Expected key of ${T::class} instead was $key"
    }

    return key
}

fun FirSession.findClassSymbol(classId: ClassId) =
    symbolProvider.getClassLikeSymbolByClassId(classId) as? FirClassSymbol

fun FirExtension.generateMutableClass(
    parentInterfaceSymbol: FirClassSymbol<*>,
    compatContext: CompatContext,
): FirRegularClassSymbol? = with(session.filters) {
    val specSymbol = snapshottableInterfaceSymbolToSpecSymbol(
        snapshottableInterfaceSymbol = parentInterfaceSymbol,
    ) ?: return@with null

    val specPrimaryConstructor = specPrimaryConstructor(specSymbol)
        ?: return@with null

    with(compatContext) {
        createNestedClassCompat(
            owner = parentInterfaceSymbol,
            name = CLASS_NAME_SNAPSHOT_MUTABLE,
            key = Snapshottable.Keys.SnapshotMutable(
                specPrimaryConstructor = specPrimaryConstructor,
            ),
        ) {
            superType(parentInterfaceSymbol.defaultType())
        }.symbol
    }
}

fun FirExtension.createFunSnapshotUpdate(
    mutableClassSymbol: FirClassSymbol<*>,
    callableId: CallableId,
    compatContext: CompatContext,
): FirNamedFunctionSymbol {
    val key = mutableClassSymbol.requireKey<Snapshottable.Keys.SnapshotMutable>()
    val symbol = with(compatContext) {
        createMemberFunctionCompat(
            owner = mutableClassSymbol,
            key = mutableClassSymbol.requireKey(),
            name = callableId.callableName,
            returnType = mutableClassSymbol.constructType(
                key.specPrimaryConstructor.typeParameterSymbols
                    .map(FirTypeParameterSymbol::toConeType)
                    .toTypedArray(),
            ),
        ) {
            key.specPrimaryConstructor.valueParameterSymbols
                .forEach { parameterSymbol ->
                    valueParameter(
                        name = parameterSymbol.name,
                        type = parameterSymbol.resolvedReturnType,
                        hasDefaultValue = true,
                    )
                }
        }
    }
    for (param in symbol.fir.valueParameters) {
        if (param.defaultValue != null) {
            param.replaceDefaultValue(buildSafeDefaultValueStub(session))
        }
    }
    return symbol
}

fun FirExtension.createFunConversion(
    key: Snapshottable.Keys.WithSpec,
    inputClassSymbol: FirClassSymbol<*>,
    outputClassSymbol: FirClassSymbol<*>,
    callableId: CallableId,
    compatContext: CompatContext,
): FirNamedFunctionSymbol = with(compatContext) {
    createMemberFunctionCompat(
        owner = inputClassSymbol,
        key = key,
        name = callableId.callableName,
        returnType = outputClassSymbol.constructType(
            typeArguments = key.specPrimaryConstructor.typeParameterSymbols
                .map(FirTypeParameterSymbol::toConeType)
                .toTypedArray(),
        ),
    )
}

fun FirExtension.maybeCreatePropertyOnInterfaceOrMutableClass(
    classSymbol: FirClassSymbol<*>,
    specSymbol: FirClassSymbol<*>,
    callableId: CallableId,
    compatContext: CompatContext,
): FirProperty? {
    val isInterface = classSymbol.isInterface
    val specProperties = specSymbol.declaredProperties(
        session,
        memberRequiredPhase = FirResolvePhase.RAW_FIR,
    )

    val parameter = specProperties
        .singleOrNull { it.name == callableId.callableName } ?: return null

    // Check if this interface is already overriding a property from it's supertype
    if (isInterface && parameter.rawStatus.isOverride) return null

    return with(compatContext) {
        createMemberPropertyCompat(
            owner = classSymbol,
            key = if (isInterface) Snapshottable.Keys.Default else classSymbol.requireKey<Snapshottable.Keys>(),
            name = callableId.callableName,
            returnType = parameter.resolvedReturnType,
            isVal = classSymbol.isInterface,
            hasBackingField = false,
        ) {
            status { isOverride = !isInterface }
            if (isInterface) modality = Modality.ABSTRACT
        }
    }
}

// Workaround for https://youtrack.jetbrains.com/issue/KT-81808
private fun buildSafeDefaultValueStub(
    session: FirSession,
    message: String = "Stub!",
): FirFunctionCall {
    return buildFunctionCall {
        this.coneTypeOrNull = session.builtinTypes.nothingType.coneType
        this.calleeReference = buildResolvedNamedReference {
            val errorFunctionSymbol = session.symbolProvider.getTopLevelFunctionSymbols(
                packageFqName = kotlinPackageFqn,
                name = Name.identifier("error"),
            ).firstOrNull {
                it.valueParameterSymbols.size == 1
            } ?: error("Could not find kotlin.error function")
            this.resolvedSymbol = errorFunctionSymbol
            this.name = errorFunctionSymbol.name
        }
        argumentList =
            buildResolvedArgumentList(
                buildArgumentList {
                    this.arguments +=
                        buildLiteralExpression(
                            source = null,
                            kind = ConstantValueKind.String,
                            value = message,
                            setType = true,
                        )
                },
                LinkedHashMap(),
            )
    }
}
