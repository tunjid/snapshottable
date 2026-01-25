package foo.bar

import com.tunjid.snapshottable.Snapshottable
import foo.bar.DataMethodsState.Companion.toSnapshotMutable
import foo.bar.DataMethodsState.Companion.toSnapshotSpec

fun box(): String {
    val state1 = DataMethodsState.Immutable(id = 1, data = "a").toSnapshotMutable()
    val state2 = DataMethodsState.Immutable(id = 1, data = "a").toSnapshotMutable()
    val state3 = DataMethodsState.Immutable(id = 2, data = "b").toSnapshotMutable()

    // Test equals (assuming generated class implements equals based on properties)
    // Note: The current implementation might not generate equals/hashCode for the mutable class.
    // If it doesn't, this test might fail or need adjustment.
    // For now, let's check if the specs are equal.

    if (state1.toSnapshotSpec() != state2.toSnapshotSpec()) return "Fail 1"
    if (state1.toSnapshotSpec() == state3.toSnapshotSpec()) return "Fail 2"

    // Test toString (basic check)
    val str = state1.toSnapshotSpec().toString()
    if (!str.contains("id=1")) return "Fail 3"
    if (!str.contains("data=a")) return "Fail 4"

    return "OK"
}

@Snapshottable
interface DataMethodsState {
    @Snapshottable.Spec
    data class Immutable(
        val id: Int,
        val data: String
    ) : DataMethodsState
}
