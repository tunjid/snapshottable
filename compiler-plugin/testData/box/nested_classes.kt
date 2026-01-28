package foo.bar

import com.tunjid.snapshottable.Snapshottable
import com.tunjid.snapshottable.SnapshottableSpec
import foo.bar.Outer.InnerState.Companion.toSnapshotMutable
import foo.bar.Outer.InnerState.Companion.toSnapshotSpec

class Outer {
    @Snapshottable
    interface InnerState {
        @SnapshottableSpec
        data class Immutable(
            val value: String
        ) : InnerState
    }
}

fun box(): String {
    val state = Outer.InnerState.Immutable(value = "inner").toSnapshotMutable()

    if (state.value != "inner") return "Fail 1"

    state.value = "changed"
    if (state.value != "changed") return "Fail 2"

    val spec = state.toSnapshotSpec()
    if (spec.value != "changed") return "Fail 3"

    return "OK"
}
