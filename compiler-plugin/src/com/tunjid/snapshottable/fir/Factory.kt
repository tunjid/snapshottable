package com.tunjid.snapshottable.fir

import com.tunjid.snapshottable.Snapshottable
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.declaredProperties
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.buildResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
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
val COMPANION_FUN_NAME_TO_SPEC = Name.identifier("toSnapshotSpec")
val COMPANION_FUN_NAME_TO_SNAPSHOT_MUTABLE = Name.identifier("toSnapshotMutable")

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

fun FirExtension.generateCompanionDeclaration(
    parentInterfaceSymbol: FirRegularClassSymbol,
): FirRegularClassSymbol? = with(session.filters) {
    if (parentInterfaceSymbol.resolvedCompanionObjectSymbol != null) return@with null

    val specSymbol = snapshottableInterfaceSymbolToSpecSymbol(
        snapshottableInterfaceSymbol = parentInterfaceSymbol,
    ) ?: return@with null

    val specPrimaryConstructor = specPrimaryConstructor(specSymbol)
        ?: return@with null

    val companion = createCompanionObject(
        owner = parentInterfaceSymbol,
        key = Snapshottable.Keys.Companion(
            parentInterfaceClassId = parentInterfaceSymbol.classId,
            specPrimaryConstructor = specPrimaryConstructor,
        ),
    )
    return companion.symbol
}

fun FirExtension.generateMutableClass(
    parentInterfaceSymbol: FirClassSymbol<*>,
): FirRegularClassSymbol? = with(session.filters) {
    val specSymbol = snapshottableInterfaceSymbolToSpecSymbol(
        snapshottableInterfaceSymbol = parentInterfaceSymbol,
    ) ?: return@with null

    val specPrimaryConstructor = specPrimaryConstructor(specSymbol)
        ?: return@with null

    createNestedClass(
        owner = parentInterfaceSymbol,
        name = CLASS_NAME_SNAPSHOT_MUTABLE,
        key = Snapshottable.Keys.SnapshotMutable(
            specPrimaryConstructor = specPrimaryConstructor,
        ),
    ) {
        superType(parentInterfaceSymbol.defaultType())
    }.symbol
}

fun FirExtension.createFunMutableMutate(
    mutableClassSymbol: FirClassSymbol<*>,
    callableId: CallableId,
): FirSimpleFunction {
    val key = mutableClassSymbol.requireKey<Snapshottable.Keys.SnapshotMutable>()
    return createMemberFunction(
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
        .apply {
            for (param in valueParameters) {
                if (param.defaultValue != null) {
                    param.replaceDefaultValue(buildSafeDefaultValueStub(session))
                }
            }
        }
}

fun FirExtension.createFunCompanionConversion(
    companionSymbol: FirClassSymbol<*>,
    inputClassSymbol: FirClassSymbol<*>,
    outputClassSymbol: FirClassSymbol<*>,
    callableId: CallableId,
): FirSimpleFunction = createMemberFunction(
    owner = companionSymbol,
    key = companionSymbol.requireKey(),
    name = callableId.callableName,
    returnType = outputClassSymbol.constructType(
        typeArguments = outputClassSymbol.typeParameterSymbols
            .map(FirTypeParameterSymbol::toConeType)
            .toTypedArray(),
    ),
) {
    extensionReceiverType {
        inputClassSymbol.constructType(
            typeArguments = inputClassSymbol.typeParameterSymbols
                .map(FirTypeParameterSymbol::toConeType)
                .toTypedArray(),
        )
    }
}

fun FirExtension.maybeCreatePropertyOnInterfaceOrMutableClass(
    classSymbol: FirClassSymbol<*>,
    specSymbol: FirClassSymbol<*>,
    callableId: CallableId,
): FirProperty? {
    val isInterface = classSymbol.isInterface
    if (isInterface) {
        val isRedeclaration = specSymbol.declaredProperties(session)
            .any { it.name == callableId.callableName && it.rawStatus.isOverride }

        if (isRedeclaration) return null
    }

    val valueParameterSymbols =
        if (isInterface) session.filters.specPrimaryConstructor(specSymbol)
            ?.valueParameterSymbols
            ?: return null
        else classSymbol.requireKey<Snapshottable.Keys.SnapshotMutable>()
            .specPrimaryConstructor
            .valueParameterSymbols

    val parameter = valueParameterSymbols
        .singleOrNull { it.name == callableId.callableName } ?: return null

    return createMemberProperty(
        owner = classSymbol,
        key = if (isInterface) Snapshottable.Keys.Default else classSymbol.requireKey(),
        name = callableId.callableName,
        returnType = parameter.resolvedReturnType,
        isVal = classSymbol.isInterface,
        hasBackingField = false,
    ) {
        status { isOverride = !isInterface }
        if (isInterface) modality = Modality.ABSTRACT
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
