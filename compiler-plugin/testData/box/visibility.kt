package foo.bar

import com.tunjid.snapshottable.Snapshottable
import foo.bar.VisibilityState.Companion.toSnapshotMutable
import foo.bar.VisibilityState.Companion.toSnapshotSpec

fun box(): String {
    val state = VisibilityState.Immutable(
        publicVal = "public",
        internalVal = "internal"
    ).toSnapshotMutable()

    if (state.publicVal != "public") return "Fail 1"
    if (state.internalVal != "internal") return "Fail 2"

    state.publicVal = "new public"
    state.internalVal = "new internal"

    if (state.publicVal != "new public") return "Fail 3"
    if (state.internalVal != "new internal") return "Fail 4"

    val spec = state.toSnapshotSpec()
    if (spec.publicVal != "new public") return "Fail 5"
    if (spec.internalVal != "new internal") return "Fail 6"

    return "OK"
}

@Snapshottable
interface VisibilityState {
    @Snapshottable.Spec
    data class Immutable(
        val publicVal: String,
        internal val internalVal: String
    ) : VisibilityState
}
