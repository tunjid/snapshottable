package foo.bar

import com.tunjid.snapshottable.Snapshottable
import foo.bar.DefaultState.Companion.toSnapshotMutable
import foo.bar.DefaultState.Companion.toSnapshotSpec

fun box(): String {
    val state = DefaultState.Immutable().toSnapshotMutable()

    if (state.name != "default") return "Fail 1"
    if (state.count != 0) return "Fail 2"

    state.update(count = 10)
    if (state.name != "default") return "Fail 3"
    if (state.count != 10) return "Fail 4"

    val spec = state.toSnapshotSpec()
    if (spec.name != "default") return "Fail 5"
    if (spec.count != 10) return "Fail 6"

    return "OK"
}

@Snapshottable
interface DefaultState {
    @Snapshottable.Spec
    data class Immutable(
        val name: String = "default",
        val count: Int = 0
    ) : DefaultState
}
