package com.tunjid.snapshottable

/**
 * Marks an interface as a candidate for generating a mutable, snapshot-backed implementation.
 *
 * The annotated interface must contain a nested class annotated with [SnapshotSpec]
 * that implements the interface. This class serves as the immutable specification for the state.
 *
 * The compiler plugin will generate:
 * 1. A nested `SnapshotMutable` class within the interface that implements the interface and
 *    delegates its properties to Compose [androidx.compose.runtime.State] state.
 * 2. Extension functions `toSnapshotMutable()` and `toSnapshotSpec()` on the interface's companion object
 *    to convert between the immutable spec and the mutable snapshot representation.
 *
 * Example:
 * ```kotlin
 * @Snapshottable
 * interface State {
 *     @SnapshotSpec
 *     data class Immutable(val count: Int) : State
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class Snapshottable

/**
 * Marks a nested class within a [Snapshottable] interface as the immutable specification
 * for the state.
 *
 * This class must:
 * - Implement the enclosing [Snapshottable] interface.
 * - Have a primary constructor where all parameters are `val` properties.
 * - All properties must be public.
 *
 * The properties defined in the primary constructor will be mirrored in the generated `SnapshotMutable` class,
 * but backed by mutable Compose state (e.g., `mutableStateOf`, `mutableIntStateOf`, etc).
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class SnapshotSpec
