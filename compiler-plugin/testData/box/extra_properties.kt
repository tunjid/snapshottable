package foo.bar

import com.tunjid.snapshottable.Snapshottable
import foo.bar.ExtraState.Companion.toSnapshotMutable
import foo.bar.ExtraState.Companion.toSnapshotSpec

fun box(): String {
    val state = ExtraState.Immutable(
        base = 1,
        extra = "extra"
    ).toSnapshotMutable()

    if (state.base != 1) return "Fail 1"
    if (state.extra != "extra") return "Fail 2"

    state.base = 2
    state.extra = "modified"

    if (state.base != 2) return "Fail 3"
    if (state.extra != "modified") return "Fail 4"

    val spec = state.toSnapshotSpec()
    if (spec.base != 2) return "Fail 5"
    if (spec.extra != "modified") return "Fail 6"

    return "OK"
}

@Snapshottable
interface <!REDECLARATION!>ExtraState<!> {
    val <!REDECLARATION!>base<!>: Int

    @Snapshottable.Spec
    data class Immutable(
        override val base: Int,
        val extra: String
    ) : ExtraState
}
