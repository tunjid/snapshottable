package foo.bar

import com.tunjid.snapshottable.Snapshottable
import com.tunjid.snapshottable.SnapshotSpec

enum class Status {
    IDLE, RUNNING, FINISHED
}

sealed class Result {
    data class Success(val data: String) : Result()
    data class Error(val message: String) : Result()
    object None : Result()
}

fun box(): String {
    val state = EnumSealedState.Immutable(
        status = Status.IDLE,
        result = Result.None
    ).toSnapshotMutable()

    if (state.status != Status.IDLE) return "Fail 1"
    if (state.result != Result.None) return "Fail 2"

    state.status = Status.RUNNING
    if (state.status != Status.RUNNING) return "Fail 3"

    state.update(result = Result.Success("Ok"))
    if (state.result != Result.Success("Ok")) return "Fail 4"

    state.update(status = Status.FINISHED, result = Result.Error("Bad"))
    if (state.status != Status.FINISHED) return "Fail 5"
    if (state.result != Result.Error("Bad")) return "Fail 6"

    val spec = state.toSnapshotSpec()
    if (spec.status != Status.FINISHED) return "Fail 7"
    if (spec.result != Result.Error("Bad")) return "Fail 8"

    return "OK"
}

@Snapshottable
interface EnumSealedState {
    @SnapshotSpec
    data class Immutable(
        val status: Status,
        val result: Result
    ) : EnumSealedState
}
