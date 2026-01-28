package foo.bar

import com.tunjid.snapshottable.Snapshottable
import com.tunjid.snapshottable.SnapshottableSpec
import foo.bar.ArrayState.Companion.toSnapshotMutable
import foo.bar.ArrayState.Companion.toSnapshotSpec

fun box(): String {
    val state = ArrayState.Immutable(
        intArray = intArrayOf(1, 2),
        stringArray = arrayOf("a", "b")
    ).toSnapshotMutable()

    if (state.intArray[0] != 1) return "Fail 1"
    if (state.stringArray[1] != "b") return "Fail 2"

    // Modifying the array content in place (since it's a mutable object reference)
    // This tests that the reference is preserved.
    state.intArray[0] = 3
    if (state.intArray[0] != 3) return "Fail 3"

    // Replacing the array
    state.stringArray = arrayOf("c", "d")
    if (state.stringArray[0] != "c") return "Fail 4"

    val spec = state.toSnapshotSpec()
    if (spec.intArray[0] != 3) return "Fail 5"
    if (spec.stringArray[0] != "c") return "Fail 6"

    return "OK"
}

@Snapshottable
interface ArrayState {
    @SnapshottableSpec
    data class Immutable(
        val intArray: IntArray,
        val stringArray: Array<String>
    ) : ArrayState {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Immutable) return false

            if (!intArray.contentEquals(other.intArray)) return false
            if (!stringArray.contentEquals(other.stringArray)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = intArray.contentHashCode()
            result = 31 * result + stringArray.contentHashCode()
            return result
        }
    }
}
