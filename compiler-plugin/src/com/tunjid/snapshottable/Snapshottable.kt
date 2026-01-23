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

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate.BuilderContext.annotated
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.GeneratedByPlugin
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object Snapshottable {
    private const val ANNOTATION = "com.tunjid.snapshottable.Snapshottable"
    private const val PARENT_ANNOTATION = "com.tunjid.snapshottable.Snapshottable.Parent"

    private val annotationFullyQualifiedName = FqName(ANNOTATION)
    val annotationClassId = ClassId.topLevel(annotationFullyQualifiedName)
    val annotationLookupPredicate = annotated(annotationFullyQualifiedName)
    val annotationDeclarationPredicate = DeclarationPredicate.create {
        annotated(FqName(ANNOTATION))
    }

    private val parentFullyQualifiedName = FqName(PARENT_ANNOTATION)
    val parentAnnotationLookupPredicate = annotated(parentFullyQualifiedName)
    val parentAnnotationDeclarationPredicate = DeclarationPredicate.create {
        annotated(FqName(PARENT_ANNOTATION))
    }

    val composeRuntimeFullyQualifiedName = FqName(fqName = "androidx.compose.runtime")
    val composeMutableStateFactory = Name.identifier("mutableStateOf")
    val composeMutableState = Name.identifier("MutableState")
    val composeStateValue = Name.identifier("value")

    object Key : GeneratedDeclarationKey() {
        override fun toString(): String = "SnapshottableKey"
    }

    val ORIGIN = GeneratedByPlugin(Key)

    internal fun Name.toJavaSetter(): Name {
        val name = asString()
        return Name.identifier("set" + name[0].uppercase() + name.substring(1))
    }
}
