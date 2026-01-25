package foo.bar

import com.tunjid.snapshottable.Snapshottable
import foo.bar.Node.Companion.toSnapshotMutable
import foo.bar.Node.Companion.toSnapshotSpec

fun box(): String {
    val node2 = Node.Immutable(value = 2, next = null)
    val node1 = Node.Immutable(value = 1, next = node2)

    val state = node1.toSnapshotMutable()

    if (state.value != 1) return "Fail 1"
    if (state.next?.<!OVERLOAD_RESOLUTION_AMBIGUITY!>value<!> != 2) return "Fail 2"

    val node3 = Node.Immutable(value = 3, next = null)
    state.next = node3

    if (state.next?.<!OVERLOAD_RESOLUTION_AMBIGUITY!>value<!> != 3) return "Fail 3"

    val spec = state.toSnapshotSpec()
    if (spec.next?.<!OVERLOAD_RESOLUTION_AMBIGUITY!>value<!> != 3) return "Fail 4"

    return "OK"
}

@Snapshottable
interface <!REDECLARATION, REDECLARATION!>Node<!> {
    val <!REDECLARATION!>value<!>: Int
    val <!REDECLARATION!>next<!>: Node?

    @Snapshottable.Spec
    data class Immutable(
        override val value: Int,
        override val next: Node?
    ) : Node
}
