/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.tunjid.snapshottable.ir

import com.tunjid.snapshottable.Snapshottable
import com.tunjid.snapshottable.Snapshottable.toJavaSetter
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.defaultValueForType
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class SnapshottableIrBodyGenerator(
    private val context: IrPluginContext
) : IrVisitorVoid() {

    private val nullableStringType = context.irBuiltIns.stringType.makeNullable()
    private val illegalStateExceptionConstructor =
        context.referenceConstructors(ClassId.topLevel(FqName("kotlin.IllegalStateException")))
            .single { constructor ->
                val parameter = constructor.owner
                    .parameters
                    .singleOrNull() ?: return@single false
                parameter.type == nullableStringType
            }

    override fun visitElement(element: IrElement) {
        when (element) {
            is IrDeclaration,
            is IrFile,
            is IrModuleFragment -> element.acceptChildrenVoid(this)

            else -> Unit
        }
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.origin == Snapshottable.ORIGIN) {
            val declarations = declaration.declarations

            val mutablePropertyBackings = declarations
                .filterIsInstance<IrProperty>()
                .map { generateBacking(declaration, it) }

            declarations.addAll(0, mutablePropertyBackings.flatMap { listOf(it.flag, it.holder) })
        }

        declaration.acceptChildrenVoid(this)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        if (declaration.origin == Snapshottable.ORIGIN && declaration.body == null) {
            declaration.body = when (declaration.name) {
                // TODO other generations

                else -> generateMutableSetter(declaration)
            }
        }
    }

    override fun visitConstructor(declaration: IrConstructor) {
        if (declaration.origin == Snapshottable.ORIGIN) {
            if (declaration.body == null) {
                declaration.body = generateDefaultConstructor(declaration)
            }
        }
    }

    /**
     * ```kotlin
     * fun setName(name: String?): Mutable {
     *     this.name = name
     *     return this
     * }
     * ```
     */
    private fun generateMutableSetter(
        function: IrSimpleFunction
    ): IrBody? {
        val receiver = function.dispatchReceiverParameter ?: return null
        val mutableClass = function.parent as? IrClass ?: return null
        val property = mutableClass.declarations.filterIsInstance<IrProperty>()
            .single { it.name.toJavaSetter() == function.name }

        val irBuilder = DeclarationIrBuilder(context, function.symbol)
        return irBuilder.irBlockBody {
            val propertySet = irCall(property.setter!!).apply {
                dispatchReceiver = irGet(receiver)
                arguments[0] = irGet(function.parameters[0])
            }

            +propertySet
            +irReturn(irGet(receiver))
        }
    }

    private fun generateDefaultConstructor(
        declaration: IrConstructor
    ): IrBody? {
        val returnType = declaration.returnType as? IrSimpleType ?: return null
        val parentClass = declaration.parent as? IrClass ?: return null
        val anySymbol = context.irBuiltIns.anyClass.owner.primaryConstructor?.symbol ?: return null

        val irBuilder = DeclarationIrBuilder(context, declaration.symbol)
        return irBuilder.irBlockBody {
            +IrDelegatingConstructorCallImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                context.irBuiltIns.anyType,
                anySymbol,
                typeArgumentsCount = 0,
            )
            +IrInstanceInitializerCallImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                parentClass.symbol,
                returnType,
            )
        }
    }

    private fun generateBacking(
        klass: IrClass,
        property: IrProperty,
    ): MutablePropertyBacking {
        val getter = requireNotNull(property.getter)
        val setter = requireNotNull(property.setter)
        property.backingField = null

        val isPrimitive = getter.returnType.isPrimitiveType()
        val backingType = when {
            isPrimitive -> getter.returnType
            else -> getter.returnType.makeNullable()
        }

        val flagField = context.irFactory.buildField {
            origin = Snapshottable.ORIGIN
            visibility = DescriptorVisibilities.PRIVATE
            name = Name.identifier("${property.name}\$SnapshottableFlag")
            type = context.irBuiltIns.booleanType
        }.apply {
            parent = klass
            initializer = context.irFactory.createExpressionBody(
                expression = false.toIrConst(context.irBuiltIns.booleanType)
            )
        }

        val holderField = context.irFactory.buildField {
            origin = Snapshottable.ORIGIN
            visibility = DescriptorVisibilities.PRIVATE
            name = Name.identifier("${property.name}\$SnapshottableHolder")
            type = backingType
        }.apply {
            parent = klass
            initializer = context.irFactory.createExpressionBody(
                expression = when (isPrimitive) {
                    true -> IrConstImpl.defaultValueForType(
                        startOffset = SYNTHETIC_OFFSET,
                        endOffset = SYNTHETIC_OFFSET,
                        type = backingType
                    )

                    false -> null.toIrConst(backingType)
                }
            )
        }

        getter.origin = Snapshottable.ORIGIN
        getter.body = DeclarationIrBuilder(context, getter.symbol).irBlockBody {
            val dispatch = getter.dispatchReceiverParameter!!
            +irIfThenElse(
                type = getter.returnType,
                condition = irGetField(irGet(dispatch), flagField),
                thenPart = irReturn(irGetField(irGet(dispatch), holderField)),
                elsePart = irThrow(
                    irCall(illegalStateExceptionConstructor).apply {
                        arguments[0] = irString("Uninitialized property '${property.name}'.")
                    }
                )
            )
        }

        setter.origin = Snapshottable.ORIGIN
        setter.body = DeclarationIrBuilder(context, setter.symbol).irBlockBody {
            val dispatch = setter.dispatchReceiverParameter!!
            +irSetField(
                receiver = irGet(dispatch),
                field = holderField,
                value = irGet(setter.parameters[0])
            )
            +irSetField(
                receiver = irGet(dispatch),
                field = flagField,
                value = true.toIrConst(context.irBuiltIns.booleanType)
            )
        }

        val mutablePropertyBacking = MutablePropertyBacking(holderField, flagField)
        property.mutablePropertyBacking = mutablePropertyBacking
        return mutablePropertyBacking
    }
}
