package foo.bar

import com.tunjid.snapshottable.Snapshottable
import com.tunjid.snapshottable.SnapshotSpec

fun box(): String {
    val state = CollectionState.Immutable(
        list = listOf("a", "b"),
        map = mapOf("key" to 1)
    ).toSnapshotMutable()

    if (state.list != listOf("a", "b")) return "Fail 1"
    if (state.map != mapOf("key" to 1)) return "Fail 2"

    state.list = listOf("c")
    if (state.list != listOf("c")) return "Fail 3"

    state.update(map = mapOf("key2" to 2))
    if (state.map != mapOf("key2" to 2)) return "Fail 4"

    val spec = state.toSnapshotSpec()
    if (spec.list != listOf("c")) return "Fail 5"
    if (spec.map != mapOf("key2" to 2)) return "Fail 6"

    return "OK"
}

@Snapshottable
interface CollectionState {
    @SnapshotSpec
    data class Immutable(
        val list: List<String>,
        val map: Map<String, Int>
    ) : CollectionState
}
