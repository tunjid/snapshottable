package foo.bar

import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable

fun box(): String {
    val state = GenericState.Immutable(
        value = 5,
        label = "five",
    ).toSnapshotMutable()

    if (state.value != 5) return "Fail 1"
    if (state.label != "five") return "Fail 2"

    state.value = 10
    if (state.value != 10) return "Fail 3"

    state.update(label = "ten")
    if (state.value != 10) return "Fail 4"
    if (state.label != "ten") return "Fail 5"

    val spec = state.toSnapshotSpec()
    if (spec.value != 10) return "Fail 6"
    if (spec.label != "ten") return "Fail 7"

    val pair = PairedState.Immutable(
        key = "k1",
        value = listOf(1, 2, 3),
    ).toSnapshotMutable()
    pair.value = listOf(4, 5)
    if (pair.toSnapshotSpec().value != listOf(4, 5)) return "Fail 8"

    val map = MapState.Immutable<String, Int>(
        entries = mapOf("a" to 1, "b" to 2),
    ).toSnapshotMutable()
    if (map.entries != mapOf("a" to 1, "b" to 2)) return "Fail 9"
    map.entries = mapOf("c" to 3)
    if (map.toSnapshotSpec().entries != mapOf("c" to 3)) return "Fail 10"
    map.update(entries = mapOf("d" to 4, "e" to 5))
    if (map.toSnapshotSpec().entries != mapOf("d" to 4, "e" to 5)) return "Fail 11"

    return "OK"
}

@Snapshottable
interface GenericState<T : Comparable<T>> {
    val value: T
    val label: String

    @SnapshotSpec
    data class Immutable<T : Comparable<T>>(
        override val value: T,
        override val label: String = "default",
    ) : GenericState<T>
}

@Snapshottable
interface PairedState<K : CharSequence, V : List<Number>> {
    val key: K
    val value: V

    @SnapshotSpec
    data class Immutable<K : CharSequence, V : List<Number>>(
        override val key: K,
        override val value: V,
    ) : PairedState<K, V>
}

@Snapshottable
interface MapState<K, V> {
    val entries: Map<K, V>

    @SnapshotSpec
    data class Immutable<K, V>(
        override val entries: Map<K, V>,
    ) : MapState<K, V>
}
