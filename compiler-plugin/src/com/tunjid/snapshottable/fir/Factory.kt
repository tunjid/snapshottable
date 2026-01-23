package com.tunjid.snapshottable.fir

import com.tunjid.snapshottable.Snapshottable
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.processAllDeclarations
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.buildResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.plugin.DeclarationBuildingContext
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.ir.util.kotlinPackageFqn
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.Variance

val CLASS_NAME_SNAPSHOT_MUTABLE = Name.identifier("SnapshotMutable")
val MEMBER_FUN_NAME_UPDATE = Name.identifier("update")
val COMPANION_FUN_NAME_TO_SPEC = Name.identifier("toSpec")
val COMPANION_FUN_NAME_TO_SNAPSHOT_MUTABLE = Name.identifier("toSnapshotMutable")

val ClassId.mutable: ClassId get() = createNestedClassId(CLASS_NAME_SNAPSHOT_MUTABLE)
val ClassId.companion: ClassId get() = createNestedClassId(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)

fun FirSession.findClassSymbol(classId: ClassId) =
    symbolProvider.getClassLikeSymbolByClassId(classId) as? FirClassSymbol

private fun DeclarationBuildingContext<*>.copyTypeParametersFrom(
    sourceSymbol: FirClassSymbol<*>,
    session: FirSession,
) {
    for (parameter in sourceSymbol.typeParameterSymbols) {
        typeParameter(
            name = parameter.name,
            variance = Variance.INVARIANT, // Type must always be invariant to support read and write access.
        ) {
            for (bound in parameter.resolvedBounds) {
                bound { typeParameters ->
                    val arguments = typeParameters.map { it.toConeType() }
                    val substitutor = substitutor(
                        sourceSymbol = sourceSymbol,
                        builderArguments = arguments,
                        session = session
                    )
                    substitutor.substituteOrSelf(bound.coneType)
                }
            }
        }
    }
}

private fun substitutor(
    sourceSymbol: FirClassLikeSymbol<*>,
    mutableClassSymbol: FirClassLikeSymbol<*>,
    session: FirSession,
): ConeSubstitutor {
    val builderArguments = mutableClassSymbol.typeParameterSymbols.map { it.toConeType() }
    return substitutor(
        sourceSymbol = sourceSymbol,
        builderArguments = builderArguments,
        session = session,
    )
}

private fun substitutor(
    sourceSymbol: FirClassLikeSymbol<*>,
    builderArguments: List<ConeKotlinType>,
    session: FirSession,
): ConeSubstitutor {
    val parameters = sourceSymbol.typeParameterSymbols
    return substitutorByMap(
        substitution = parameters.zip(builderArguments).toMap(),
        useSiteSession = session,
    )
}

fun FirSession.getPrimaryConstructorValueParameters(
    classSymbol: FirClassSymbol<*>,
): List<FirValueParameterSymbol> {
    val declarationSymbols = mutableListOf<FirConstructorSymbol>()
    classSymbol.processAllDeclarations(session = this) { symbol ->
        if (symbol is FirConstructorSymbol && symbol.isPrimary) declarationSymbols.add(symbol)
    }
    val outerPrimaryConstructor = declarationSymbols.firstOrNull() ?: return emptyList()

    return outerPrimaryConstructor.valueParameterSymbols
}

fun FirExtension.generateMutableClass(
    parentInterfaceSymbol: FirClassSymbol<*>,
    mutableClassSymbol: FirClassSymbol<*>,
    snapshottableClassSymbol: FirClassSymbol<*>,
): FirRegularClass {
    return createNestedClass(
        owner = mutableClassSymbol,
        name = CLASS_NAME_SNAPSHOT_MUTABLE,
        key = Snapshottable.Key,
    ) {
        superType(parentInterfaceSymbol.defaultType())
        copyTypeParametersFrom(
            sourceSymbol = snapshottableClassSymbol,
            session = session
        )
    }
}

fun FirExtension.createFunMutableMutate(
    mutableClassSymbol: FirClassSymbol<*>,
    snapshottableClassSymbol: FirClassSymbol<*>,
    callableId: CallableId,
): FirSimpleFunction {
    val substitutor = substitutor(
        sourceSymbol = snapshottableClassSymbol,
        mutableClassSymbol = mutableClassSymbol,
        session = session,
    )

    return createMemberFunction(
        owner = mutableClassSymbol,
        key = Snapshottable.Key,
        name = callableId.callableName,
        returnType = mutableClassSymbol.constructType(
            mutableClassSymbol.typeParameterSymbols
                .map(FirTypeParameterSymbol::toConeType)
                .toTypedArray()
        ),
    ) {
        session.getPrimaryConstructorValueParameters(snapshottableClassSymbol)
            .forEach { parameterSymbol ->
                valueParameter(
                    name = parameterSymbol.name,
                    type = substitutor.substituteOrSelf(parameterSymbol.resolvedReturnType),
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
    key = Snapshottable.Key,
    name = callableId.callableName,
    returnType = outputClassSymbol.constructType(
        typeArguments = outputClassSymbol.typeParameterSymbols
            .map(FirTypeParameterSymbol::toConeType)
            .toTypedArray()
    ),
) {
    extensionReceiverType {
        inputClassSymbol.constructType(
            typeArguments = inputClassSymbol.typeParameterSymbols
                .map(FirTypeParameterSymbol::toConeType)
                .toTypedArray()
        )
    }
}

fun FirExtension.createInterfaceOrMutableProperty(
    classSymbol: FirClassSymbol<*>,
    snapshottableClassSymbol: FirClassSymbol<*>,
    callableId: CallableId
): FirProperty? {
    val parameter = session.getPrimaryConstructorValueParameters(
        classSymbol = snapshottableClassSymbol
    )
        .singleOrNull { it.name == callableId.callableName } ?: return null
    val substitutor = substitutor(
        sourceSymbol = snapshottableClassSymbol,
        mutableClassSymbol = classSymbol,
        session = session,
    )
    return createMemberProperty(
        owner = classSymbol,
        key = Snapshottable.Key,
        name = callableId.callableName,
        returnType = substitutor.substituteOrSelf(parameter.resolvedReturnType),
        isVal = classSymbol.isInterface,
        hasBackingField = false,
    ) {
        status { isOverride = !classSymbol.isInterface }
        if (classSymbol.isInterface) modality = Modality.ABSTRACT
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
                name = Name.identifier("error")
            ).first {
                it.valueParameterSymbols.size == 1
            }
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
