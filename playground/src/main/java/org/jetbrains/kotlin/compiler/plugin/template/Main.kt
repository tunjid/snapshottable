package org.jetbrains.kotlin.compiler.plugin.template

import com.tunjid.snapshottable.Snapshottable
import org.jetbrains.kotlin.compiler.plugin.template.State.Companion.toSnapshotMutable
import org.jetbrains.kotlin.compiler.plugin.template.State.Companion.toSnapshotSpec
import java.util.*

// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        println("Hello and welcome!")

        val state = state()
        println(state.toSnapshotSpec())

        state.activityName = "run"
        println(state.toSnapshotSpec())

        state.update(
            activityName = "sprint",
            stepCount = 8,
        )
        println(state.toSnapshotSpec())

        state.update(stepCount = 14)
        println(state.toSnapshotSpec())

        state.update(startTimeStamp = Date().time)
        println(state.toSnapshotSpec())

        state.update(totalDistanceInMiles = 1.2f)
        println(state.toSnapshotSpec())

        println(state.toSnapshotSpec().toSnapshotMutable())
        println(state.toSnapshotSpec().toSnapshotMutable().toSnapshotSpec())
    }
}

@Snapshottable
interface State {
    @Snapshottable.Spec
    data class Immutable(
        val activityName: String = "jog",
        val stepCount: Int = 42,
        val startTimeStamp: Long = 1700923000L,
        val totalDistanceInMiles: Float = 45.5f,
        val stepsPerSecond: Double = 1.99234
    ) : State
}

fun state(): State.SnapshotMutable {
    return State.SnapshotMutable(
        activityName = "walk",
        stepCount = 25,
        startTimeStamp = Date().time,
        totalDistanceInMiles = 45.5f,
        stepsPerSecond = 1.99234,
    )
}
