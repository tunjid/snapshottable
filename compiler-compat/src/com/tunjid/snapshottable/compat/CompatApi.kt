package com.tunjid.snapshottable.compat

/**
 * Documents an entry on [CompatContext] that exists because the underlying Kotlin compiler API
 * shape changed at [since]. Purely informational — useful for explaining why the wrapper exists
 * and for grepping the surface when bumping the minimum supported compiler version.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
public annotation class CompatApi(
    public val since: String,
    public val reason: Reason,
    public val message: String = "",
) {
    public enum class Reason {
        /** The native API was deleted outright. */
        DELETED,

        /** The native API was renamed (class, function, or member). */
        RENAMED,

        /** The native API exists but its signature / ABI changed. */
        ABI_CHANGE,

        /** Behaviour-only difference: signature is stable but semantics or availability moved. */
        COMPAT,
    }
}
