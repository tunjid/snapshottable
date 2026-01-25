package foo.bar

import com.tunjid.snapshottable.Snapshottable
import foo.bar.State.Companion.toSnapshotMutable
import foo.bar.State.Companion.toSnapshotSpec

fun box() {
    val state = State.Immutable().toSnapshotMutable()
    println(state.toSnapshotSpec())

    state.activity = "run"
    println(state.toSnapshotSpec())

    state.update(
        activity = "sprint",
        stepCount = 8,
    )
    println(state.toSnapshotSpec())

    state.update(stepCount = 14)
    println(state.toSnapshotSpec())

    state.update(startTimeStamp = 1700923400L)
    println(state.toSnapshotSpec())

    state.update(totalDistanceInMiles = 1.2f)
    println(state.toSnapshotSpec())

    println(state.toSnapshotSpec().toSnapshotMutable())
    println(state.toSnapshotSpec().toSnapshotMutable().toSnapshotSpec())
}

@Snapshottable
interface State {
    @Snapshottable.Spec
    data class Immutable(
        val activity: String = "jog",
        val stepCount: Int = 42,
        val startTimeStamp: Long = 1700923000L,
        val totalDistanceInMiles: Float = 45.5f,
        val stepsPerSecond: Double = 0.4,
    ) : State
}

fun state(): State.SnapshotMutable {
    return State.SnapshotMutable(
        activity = "walk",
        stepCount = 25,
        startTimeStamp = 1700923000L,
        totalDistanceInMiles = 45.5f,
        stepsPerSecond = 0.3,
    )
}
