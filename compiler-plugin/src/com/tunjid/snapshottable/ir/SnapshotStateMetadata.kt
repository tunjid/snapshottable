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

private val SnapshotStatePackageName = FqName(fqName = "androidx.compose.runtime")

private val SnapshotMutableStateFactory = Name.identifier("mutableStateOf")
private val SnapshotMutableState = Name.identifier("MutableState")
private val SnapshotStateValue = Name.identifier("value")

private val SnapshotMutableIntStateFactory = Name.identifier("mutableIntStateOf")
private val SnapshotMutableIntState = Name.identifier("MutableIntState")
private val SnapshotIntStateValue = Name.identifier("intValue")

private val SnapshotMutableFloatStateFactory = Name.identifier("mutableFloatStateOf")
private val SnapshotMutableFloatState = Name.identifier("MutableFloatState")
private val SnapshotFloatStateValue = Name.identifier("floatValue")

private val SnapshotMutableLongStateFactory = Name.identifier("mutableLongStateOf")
private val SnapshotMutableLongState = Name.identifier("MutableLongState")
private val SnapshotLongStateValue = Name.identifier("longValue")

private val SnapshotMutableDoubleStateFactory = Name.identifier("mutableDoubleStateOf")
private val SnapshotMutableDoubleState = Name.identifier("MutableDoubleState")
private val SnapshotDoubleStateValue = Name.identifier("doubleValue")

fun IrPluginContext.snapshotStateMetadata(
    backingType: IrType
): SnapshotStateMetadata = when {
    backingType.isInt() -> {
        val snapshotStateClass = snapshotStateClass(
            stateClassName = SnapshotMutableIntState
        )
        SnapshotStateMetadata(
            factoryFunction = snapshotStateFactory(
                stateFactoryMethodName = SnapshotMutableIntStateFactory
            ),
            snapshotStateClass = snapshotStateClass,
            valueProperty = snapshotStateClass.snapshotValuePropertySymbol(
                valueName = SnapshotIntStateValue,
            ),
            type = snapshotStateClass.typeWith(),
        )
    }

    backingType.isFloat() -> {
        val snapshotStateClass = snapshotStateClass(
            stateClassName = SnapshotMutableFloatState
        )
        SnapshotStateMetadata(
            factoryFunction = snapshotStateFactory(
                stateFactoryMethodName = SnapshotMutableFloatStateFactory
            ),
            snapshotStateClass = snapshotStateClass,
            valueProperty = snapshotStateClass.snapshotValuePropertySymbol(
                valueName = SnapshotFloatStateValue,
            ),
            type = snapshotStateClass.typeWith(),
        )
    }

    backingType.isLong() -> {
        val snapshotStateClass = snapshotStateClass(
            stateClassName = SnapshotMutableLongState
        )
        SnapshotStateMetadata(
            factoryFunction = snapshotStateFactory(
                stateFactoryMethodName = SnapshotMutableLongStateFactory
            ),
            snapshotStateClass = snapshotStateClass,
            valueProperty = snapshotStateClass.snapshotValuePropertySymbol(
                valueName = SnapshotLongStateValue,
            ),
            type = snapshotStateClass.typeWith(),
        )
    }

    backingType.isDouble() -> {
        val snapshotStateClass = snapshotStateClass(
            stateClassName = SnapshotMutableDoubleState
        )
        SnapshotStateMetadata(
            factoryFunction = snapshotStateFactory(
                stateFactoryMethodName = SnapshotMutableDoubleStateFactory
            ),
            snapshotStateClass = snapshotStateClass,
            valueProperty = snapshotStateClass.snapshotValuePropertySymbol(
                valueName = SnapshotDoubleStateValue,
            ),
            type = snapshotStateClass.typeWith(),
        )
    }

    else -> {
        val snapshotStateClass = snapshotStateClass(
            stateClassName = SnapshotMutableState
        )
        SnapshotStateMetadata(
            factoryFunction = snapshotStateFactory(
                stateFactoryMethodName = SnapshotMutableStateFactory
            ),
            snapshotStateClass = snapshotStateClass(
                stateClassName = SnapshotMutableState
            ),
            valueProperty = snapshotStateClass.snapshotValuePropertySymbol(
                valueName = SnapshotStateValue
            ),
            type = snapshotStateClass.typeWith(backingType),
        )
    }
}

private fun IrPluginContext.snapshotStateFactory(
    stateFactoryMethodName: Name
): IrSimpleFunctionSymbol = referenceFunctions(
    CallableId(
        packageName = SnapshotStatePackageName,
        callableName = stateFactoryMethodName,
    )
).first { it.owner.parameters.isNotEmpty() } // Simple check for the one with args

private fun IrPluginContext.snapshotStateClass(
    stateClassName: Name
): IrClassSymbol = referenceClass(
    ClassId(
        packageFqName = SnapshotStatePackageName,
        topLevelName = stateClassName,
    )
) ?: error("MutableState not found")

private fun IrClassSymbol.snapshotValuePropertySymbol(
    valueName: Name,
): IrPropertySymbol = (owner.properties
    .find { it.name == valueName }
    ?.symbol
    ?: error("MutableState.value property not found"))