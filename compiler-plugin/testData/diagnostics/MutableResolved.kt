// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
// ENABLE_JAVA_SETTERS

package diagnostics

import com.tunjid.snapshottable.Snapshottable
import diagnostics.State.Companion.toSnapshotMutable
import diagnostics.State.Companion.toSnapshotSpec

@Snapshottable
interface State {
    @Snapshottable.Spec
    data class Immutable(
        val activity: String = "jog",
        val stepCount: Int = 42,
        val startTimeStamp: Long = 1700923000L,
        val totalDistanceInMiles: Float = 45.5f,
        val stepsPerSecond: Double = 1.99234,
    ) : State
}

fun state1(): State.SnapshotMutable {
    return State.Immutable()
        .toSnapshotMutable()
        .update(
            activity = "dash",
            stepsPerSecond = 0.7,
            stepCount = 9,
        )
}

fun person2(): State.SnapshotMutable {
    return State.SnapshotMutable(
        activity = "walk",
        stepCount = 25,
        startTimeStamp = 1769355431964,
        totalDistanceInMiles = 45.5f,
        stepsPerSecond = 0.1,
    )
        .update(
            activity = "sprint",
        )
}
