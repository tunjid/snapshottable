package com.tunjid.snapshottable

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class Snapshottable(
    val source: KClass<*>,
)
