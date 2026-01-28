package foo.bar

import com.tunjid.snapshottable.Snapshottable
import com.tunjid.snapshottable.SnapshotSpec
import foo.bar.InheritanceState.Companion.toSnapshotMutable
import foo.bar.InheritanceState.Companion.toSnapshotSpec

interface Base {
    val id: String
}

fun box(): String {
    val state = InheritanceState.Immutable(
        id = "1",
        name = "Name"
    ).toSnapshotMutable()

    if (state.id != "1") return "Fail 1"
    if (state.name != "Name") return "Fail 2"

    state.id = "2"
    state.name = "New Name"

    if (state.id != "2") return "Fail 3"
    if (state.name != "New Name") return "Fail 4"

    val spec = state.toSnapshotSpec()
    if (spec.id != "2") return "Fail 5"
    if (spec.name != "New Name") return "Fail 6"

    return "OK"
}

@Snapshottable
interface InheritanceState : Base {
    @SnapshotSpec
    data class Immutable(
        override val id: String,
        val name: String
    ) : InheritanceState
}
