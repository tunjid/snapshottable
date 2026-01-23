/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.tunjid.snapshottable.ir

import com.tunjid.snapshottable.Snapshottable
import com.tunjid.snapshottable.fir.UPDATE_FUN_NAME
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.hasDefaultValue
import org.jetbrains.kotlin.ir.util.nonDispatchParameters
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name

class SnapshottableIrBodyGenerator(
    private val context: IrPluginContext
) : IrVisitorVoid() {

    override fun visitElement(
        element: IrElement
    ) {
        when (element) {
            is IrDeclaration,
            is IrFile,
            is IrModuleFragment -> element.acceptChildrenVoid(this)

            else -> Unit
        }
    }

    override fun visitClass(
        declaration: IrClass
    ) {
        if (declaration.origin == Snapshottable.ORIGIN) {
            val declarations = declaration.declarations

            val mutablePropertyBackings = declarations
                .filterIsInstance<IrProperty>()
                .map { generateBacking(declaration, it, context) }

            declarations.addAll(0, mutablePropertyBackings)
        }

        declaration.acceptChildrenVoid(this)
    }

    override fun visitSimpleFunction(
        declaration: IrSimpleFunction
    ) {
        if (declaration.origin == Snapshottable.ORIGIN && declaration.body == null) {
            declaration.body = when (declaration.name) {
                UPDATE_FUN_NAME -> generateUpdateFunction(declaration)
                else -> declaration.body
            }
        }
    }

    override fun visitConstructor(
        declaration: IrConstructor
    ) {
        if (declaration.origin == Snapshottable.ORIGIN) {
            if (declaration.body == null) {
                declaration.body = generateDefaultConstructor(declaration)
            }
        }
    }

    private fun generateUpdateFunction(
        function: IrSimpleFunction
    ): IrBody? {
        val receiver = function.dispatchReceiverParameter ?: return null
        val mutableClass = function.parent as? IrClass ?: return null
        val properties = mutableClass.declarations.filterIsInstance<IrProperty>()

        val irBuilder = MutableStateFunctionBuilder(
            context = context,
            function = function,
        )
        return irBuilder.apply {
            function.parameters
                .filter { it.kind == IrParameterKind.Regular && it.hasDefaultValue() }
                .forEach { irValueParameter ->
                    irValueParameter.defaultValue = defaultValue(irValueParameter)
                }
            function.nonDispatchParameters.forEach { parameter ->
                val property = properties.single {
                    it.name == parameter.name
                }
                val setter = requireNotNull(property.setter)

                +irCall(setter).apply {
                    dispatchReceiver = irGet(receiver)
                    arguments[MutableClassSetterArgumentIndex] = irGet(parameter)
                }
            }
            +irReturn(irGet(receiver))
        }
            .doBuild()
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
        context: IrPluginContext,
    ): IrField {
        val getter = requireNotNull(property.getter)
        val setter = requireNotNull(property.setter)
        property.backingField = null

        val backingType = getter.returnType

        val primaryConstructor = klass.primaryConstructor
            ?: error("Class does not have a primary constructor")

        val targetValueParameter: IrValueParameter = primaryConstructor.parameters
            .firstOrNull { it.name == property.name }
            ?: error("Constructor parameter 'myParam' not found")

        val snapshotStateMetadata = context.snapshotStateMetadata(backingType)

        val builder = DeclarationIrBuilder(context, klass.symbol)
        val holderField = context.irFactory.buildField {
            origin = Snapshottable.ORIGIN
            visibility = DescriptorVisibilities.PRIVATE
            name = Name.identifier("${property.name}\$SnapshottableHolder")
            type = snapshotStateMetadata.type
            isFinal = true
        }.apply {
            parent = klass
            initializer = factory.createExpressionBody(
                builder.irCall(snapshotStateMetadata.factoryFunction).apply {
                    if (snapshotStateMetadata.hasBackingType) typeArguments[0] = backingType
                    arguments[0] = builder.irGet(targetValueParameter)
                }
            )
        }

        getter.origin = Snapshottable.ORIGIN
        getter.body = DeclarationIrBuilder(
            generatorContext = context,
            symbol = getter.symbol
        ).irBlockBody {
            val dispatch = getter.dispatchReceiverParameter!!
            +irReturn(
                irCall(snapshotStateMetadata.valueProperty.owner.getter!!).apply {
                    dispatchReceiver = irGetField(
                        receiver = irGet(dispatch),
                        field = holderField
                    )
                }
            )
        }

        setter.origin = Snapshottable.ORIGIN
        setter.body = DeclarationIrBuilder(
            generatorContext = context,
            symbol = setter.symbol
        ).irBlockBody {
            val dispatch = setter.dispatchReceiverParameter!!
            +irCall(snapshotStateMetadata.valueProperty.owner.setter!!).apply {
                dispatchReceiver = irGetField(
                    receiver = irGet(dispatch),
                    field = holderField
                )
                arguments[MutableClassSetterArgumentIndex] = irGet(setter.nonDispatchParameters[0])
            }
        }

        return holderField
    }
}

private const val MutableClassSetterArgumentIndex = 1