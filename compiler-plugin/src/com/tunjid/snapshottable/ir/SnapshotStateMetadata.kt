package com.tunjid.snapshottable.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isDouble
import org.jetbrains.kotlin.ir.types.isFloat
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.types.isLong
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

data class SnapshotStateMetadata(
    val factoryFunction: IrSimpleFunctionSymbol,
    val snapshotStateClass: IrClassSymbol,
    val valueProperty: IrPropertySymbol,
    val type: IrSimpleType,
)

private val composeRuntimeFullyQualifiedName = FqName(fqName = "androidx.compose.runtime")

private val composeMutableStateFactory = Name.identifier("mutableStateOf")
private val composeMutableState = Name.identifier("MutableState")

private val composeMutableIntStateFactory = Name.identifier("mutableIntStateOf")
private val composeMutableIntState = Name.identifier("MutableIntState")

private val composeMutableFloatStateFactory = Name.identifier("mutableFloatStateOf")
private val composeMutableFloatState = Name.identifier("MutableFloatState")

private val composeMutableLongStateFactory = Name.identifier("mutableLongStateOf")
private val composeMutableLongState = Name.identifier("MutableLongState")

private val composeMutableDoubleStateFactory = Name.identifier("mutableDoubleStateOf")
private val composeMutableDoubleState = Name.identifier("MutableDoubleState")


val composeStateValue = Name.identifier("value")

fun IrPluginContext.snapshotStateMetadata(
    backingType: IrType
): SnapshotStateMetadata = when {
    backingType.isInt() -> {
        val snapshotStateClass = snapshotStateClass(
            stateClassName = composeMutableIntState
        )
        SnapshotStateMetadata(
            factoryFunction = snapshotStateFactory(
                stateFactoryMethodName = composeMutableIntStateFactory
            ),
            snapshotStateClass = snapshotStateClass,
            valueProperty = snapshotStateClass.snapshotValuePropertySymbol(),
            type = snapshotStateClass.typeWith(),
        )
    }

    backingType.isFloat() -> {
        val snapshotStateClass = snapshotStateClass(
            stateClassName = composeMutableFloatState
        )
        SnapshotStateMetadata(
            factoryFunction = snapshotStateFactory(
                stateFactoryMethodName = composeMutableFloatStateFactory
            ),
            snapshotStateClass = snapshotStateClass,
            valueProperty = snapshotStateClass.snapshotValuePropertySymbol(),
            type = snapshotStateClass.typeWith(),
        )
    }

    backingType.isLong() -> {
        val snapshotStateClass = snapshotStateClass(
            stateClassName = composeMutableLongState
        )
        SnapshotStateMetadata(
            factoryFunction = snapshotStateFactory(
                stateFactoryMethodName = composeMutableLongStateFactory
            ),
            snapshotStateClass = snapshotStateClass,
            valueProperty = snapshotStateClass.snapshotValuePropertySymbol(),
            type = snapshotStateClass.typeWith(),
        )
    }

    backingType.isDouble() -> {
        val snapshotStateClass = snapshotStateClass(
            stateClassName = composeMutableDoubleState
        )
        SnapshotStateMetadata(
            factoryFunction = snapshotStateFactory(
                stateFactoryMethodName = composeMutableDoubleStateFactory
            ),
            snapshotStateClass = snapshotStateClass,
            valueProperty = snapshotStateClass.snapshotValuePropertySymbol(),
            type = snapshotStateClass.typeWith(),
        )
    }

    else -> {
        val snapshotStateClass = snapshotStateClass(
            stateClassName = composeMutableState
        )
        SnapshotStateMetadata(
            factoryFunction = snapshotStateFactory(
                stateFactoryMethodName =  composeMutableStateFactory
            ),
            snapshotStateClass = snapshotStateClass(
                stateClassName = composeMutableState
            ),
            valueProperty = snapshotStateClass.snapshotValuePropertySymbol(),
            type = snapshotStateClass.typeWith(backingType),
        )
    }
}

private fun IrPluginContext.snapshotStateFactory(
    stateFactoryMethodName: Name
): IrSimpleFunctionSymbol = referenceFunctions(
    CallableId(
        packageName = composeRuntimeFullyQualifiedName,
        callableName = stateFactoryMethodName,
    )
).first { it.owner.parameters.isNotEmpty() } // Simple check for the one with args

private fun IrPluginContext.snapshotStateClass(
    stateClassName: Name
): IrClassSymbol = referenceClass(
    ClassId(
        packageFqName = composeRuntimeFullyQualifiedName,
        topLevelName = stateClassName,
    )
) ?: error("MutableState not found")

private fun IrClassSymbol.snapshotValuePropertySymbol(): IrPropertySymbol = (owner.properties
    .find { it.name == composeStateValue }
    ?.symbol
    ?: error("MutableState.value property not found"))