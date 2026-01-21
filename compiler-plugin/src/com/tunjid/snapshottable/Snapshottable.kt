/*
 * Copyright (C) 2022 Brian Norman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.snapshottable

import com.tunjid.snapshottable.fir.findClassSymbol
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate.BuilderContext.annotated
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.GeneratedByPlugin
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object Snapshottable {
    val ANNOTATION_FQ_NAME = FqName("com.tunjid.snapshottable.Snapshottable")
    val ANNOTATION_CLASS_ID = ClassId.topLevel(ANNOTATION_FQ_NAME)
    val ANNOTATION_PREDICATE = annotated(ANNOTATION_FQ_NAME)
    val SOURCE_NAME = Name.identifier("source")

    val SNAPSHOTTABLE_PREDICATE = DeclarationPredicate.create {
        annotated(FqName("com.tunjid.snapshottable.Snapshottable"))
    }

    val HAS_SNAPSHOTTABLE_PREDICATE = DeclarationPredicate.create {
        hasAnnotated(FqName("com.tunjid.snapshottable.Snapshottable"))
    }

    object Key : GeneratedDeclarationKey() {
        override fun toString(): String = "SnapshottableKey"
    }

    data class Pair(
        val annotatedClassSymbol: FirRegularClassSymbol,
        val sourceClassType: ConeKotlinType,
    )

    val ORIGIN = GeneratedByPlugin(Key)

    /**
     * Fetches the class that the Snapshottable interface mirrors
     */
    internal fun FirSession.snapshottableSourceSymbol(
        snapshottableSymbol: FirClassifierSymbol<*>?,
    ): FirClassSymbol<*>? {
        snapshottableSymbol ?: return null
        val sourceAnnotationClassCall = snapshottableSymbol.resolvedAnnotationsWithArguments
            .getAnnotationByClassId(
                classId = ANNOTATION_CLASS_ID,
                session = this
            )
            ?.argumentMapping
            ?.mapping
            ?.get(SOURCE_NAME)
            ?: return null

        // 3. Resolve the type of the class reference.
        // The typeRef of the argument is usually KClass<T>, so we look at the argument of that type.
        // A more direct way in recent FIR versions is usually checking the type of the reference:
        val sourceConeType = when (sourceAnnotationClassCall) {
            is FirGetClassCall -> sourceAnnotationClassCall.argument.resolvedType
            else -> null
        } ?: return null

        return sourceConeType.classId
            ?.let(::findClassSymbol)
    }

    internal fun Name.toJavaSetter(): Name {
        val name = asString()
        return Name.identifier("set" + name[0].uppercase() + name.substring(1))
    }
}
