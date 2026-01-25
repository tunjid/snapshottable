package foo.bar

import com.tunjid.snapshottable.Snapshottable
import foo.bar.NullableState.Companion.toSnapshotMutable
import foo.bar.NullableState.Companion.toSnapshotSpec

fun box(): String {
    val state = NullableState.Immutable(
        name = null,
        count = null
    ).toSnapshotMutable()

    if (state.name != null) return "Fail 1"
    if (state.count != null) return "Fail 2"

    state.name = "test"
    if (state.name != "test") return "Fail 3"

    state.update(count = 5)
    if (state.count != 5) return "Fail 4"

    state.update(name = null)
    if (state.name != null) return "Fail 5"

    val spec = state.toSnapshotSpec()
    if (spec.name != null) return "Fail 6"
    if (spec.count != 5) return "Fail 7"

    return "OK"
}

@Snapshottable
interface NullableState {
    @Snapshottable.Spec
    data class Immutable(
        val name: String?,
        val count: Int?
    ) : NullableState
}
