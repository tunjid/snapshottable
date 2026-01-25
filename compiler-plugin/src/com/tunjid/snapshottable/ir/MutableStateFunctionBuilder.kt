package com.tunjid.snapshottable.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.util.isFinalClass

class MutableStateFunctionBuilder(
    context: IrPluginContext,
    private val function: IrSimpleFunction,
) : IrBlockBodyBuilder(
    context = context,
    scope = Scope(function.symbol),
    startOffset = UNDEFINED_OFFSET,
    endOffset = UNDEFINED_OFFSET,
) {

    private val irClass = function.parent as IrClass

    private fun IrSimpleFunction.irThis(): IrExpression {
        val irDispatchReceiverParameter = dispatchReceiverParameter!!
        return IrGetValueImpl(
            startOffset,
            endOffset,
            irDispatchReceiverParameter.type,
            irDispatchReceiverParameter.symbol,
        )
    }

    fun defaultValue(
        parameter: IrValueParameter,
    ): IrExpressionBody {
        val property = irClass.declarations
            .filterIsInstance<IrProperty>()
            .first { irProperty ->
                irProperty.name == parameter.name
            }
        return irExprBody(
            irGetProperty(
                receiver = function.irThis(),
                property = property,
            ),
        )
    }

    private fun irGetProperty(
        receiver: IrExpression,
        property: IrProperty,
    ): IrExpression {
        // In some JVM-specific cases, such as when 'allopen' compiler plugin is applied,
        // data classes and corresponding properties can be non-final.
        // We should use getters for such properties (see KT-41284).
        val backingField = property.backingField
        return if (irClass.isFinalClass && backingField != null) {
            irGetField(receiver, backingField)
        } else {
            irCall(property.getter!!).apply {
                arguments[0] = receiver
            }
        }
    }
}
