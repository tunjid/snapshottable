package foo.bar

import com.tunjid.snapshottable.Snapshottable
import foo.bar.GenericState.Companion.toSnapshotMutable
import foo.bar.GenericState.Companion.toSnapshotSpec

fun box(): String {
    val state = GenericState.Immutable(
        item = "Hello",
        value = 10
    ).<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toSnapshotMutable<!>()

    if (state.item != "Hello") return "Fail 1"
    if (state.value != 10) return "Fail 2"

    state.item = <!ASSIGNMENT_TYPE_MISMATCH!>"World"<!>
    if (state.item != "World") return "Fail 3"

    state.update(value = <!ARGUMENT_TYPE_MISMATCH!>20<!>)
    if (state.value != 20) return "Fail 4"

    val spec = state.toSnapshotSpec()
    if (spec.item != "World") return "Fail 5"
    if (spec.value != 20) return "Fail 6"

    return "OK"
}

@Snapshottable
interface GenericState<T, R> {
    @Snapshottable.Spec
    data class Immutable<T, R>(
        val item: T,
        val value: R
    ) : GenericState<T, R>
}
