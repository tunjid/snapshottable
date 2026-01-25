package foo.bar

import com.tunjid.snapshottable.Snapshottable
import foo.bar.ComplexState.Companion.toSnapshotMutable
import foo.bar.ComplexState.Companion.toSnapshotSpec

data class Address(val street: String, val city: String)

fun box(): String {
    val initialAddress = Address("123 Main St", "City")
    val state = ComplexState.Immutable(
        user = "User",
        address = initialAddress
    ).toSnapshotMutable()

    if (state.address != initialAddress) return "Fail 1"

    val newAddress = Address("456 Elm St", "Town")
    state.address = newAddress

    if (state.address != newAddress) return "Fail 2"

    state.update(user = "New User")
    if (state.user != "New User") return "Fail 3"
    if (state.address != newAddress) return "Fail 4"

    val spec = state.toSnapshotSpec()
    if (spec.user != "New User") return "Fail 5"
    if (spec.address != newAddress) return "Fail 6"

    return "OK"
}

@Snapshottable
interface ComplexState {
    @Snapshottable.Spec
    data class Immutable(
        val user: String,
        val address: Address
    ) : ComplexState
}
