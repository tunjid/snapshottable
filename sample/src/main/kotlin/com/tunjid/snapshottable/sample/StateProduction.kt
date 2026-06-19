package com.tunjid.snapshottable.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val DEFAULT_SECTORS: List<Sector> = Sector.entries.take(2)

@Composable
fun produceState(
    mode: Mode,
    repository: StockRepository,
    actions: ReceiveChannel<Action>,
): State = when (mode) {
    Mode.SnapshotMutable -> produceSnapshotState(repository, actions)
    Mode.Immutable -> produceImmutableState(repository, actions)
}

@Composable
private fun produceSnapshotState(
    repository: StockRepository,
    actions: ReceiveChannel<Action>,
): State {
    val scope = rememberCoroutineScope()
    val state = remember {
        State.Immutable(
            selectedSectors = emptyList(),
            stockStates = emptyList(),
        ).toSnapshotMutable()
    }
    val jobs = remember { mutableMapOf<Sector, Job>() }

    LaunchedEffect(Unit) {
        fun startSector(sector: Sector) {
            if (sector in state.selectedSectors) return
            val holder = StockState.Immutable(
                sector = sector,
                stocks = emptyList(),
            ).toSnapshotMutable()
            state.selectedSectors = state.selectedSectors + sector
            state.stockStates = state.stockStates + holder
            jobs[sector] = scope.launch {
                repository.stocks(sector).collectLatest { fresh ->
                    holder.stocks = fresh
                }
            }
        }

        fun stopSector(sector: Sector) {
            jobs.remove(sector)?.cancel()
            state.selectedSectors = state.selectedSectors - sector
            state.stockStates = state.stockStates.filterNot { it.sector == sector }
        }

        DEFAULT_SECTORS.forEach(::startSector)

        for (action in actions) {
            when (action) {
                is Action.ToggleSector -> {
                    val sector = action.sector
                    if (sector in state.selectedSectors) {
                        stopSector(sector)
                    } else {
                        startSector(sector)
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { jobs.values.forEach(Job::cancel) }
    }
    return state
}

@Composable
private fun produceImmutableState(
    repository: StockRepository,
    actions: ReceiveChannel<Action>,
): State {
    val scope = rememberCoroutineScope()
    var snapshot by remember {
        mutableStateOf(
            State.Immutable(
                selectedSectors = emptyList(),
                stockStates = emptyList(),
            ),
        )
    }
    val jobs = remember { mutableMapOf<Sector, Job>() }

    LaunchedEffect(Unit) {
        fun startSector(sector: Sector) {
            if (sector in snapshot.selectedSectors) return
            snapshot = snapshot.copy(
                selectedSectors = snapshot.selectedSectors + sector,
                stockStates = snapshot.stockStates +
                    StockState.Immutable(sector, emptyList()),
            )
            jobs[sector] = scope.launch {
                repository.stocks(sector).collectLatest { fresh ->
                    val s = snapshot
                    snapshot = s.copy(
                        stockStates = s.stockStates.map { ss ->
                            if (ss.sector == sector) {
                                StockState.Immutable(sector, fresh)
                            } else {
                                ss
                            }
                        },
                    )
                }
            }
        }

        fun stopSector(sector: Sector) {
            jobs.remove(sector)?.cancel()
            snapshot = snapshot.copy(
                selectedSectors = snapshot.selectedSectors - sector,
                stockStates = snapshot.stockStates.filterNot { it.sector == sector },
            )
        }

        DEFAULT_SECTORS.forEach(::startSector)

        for (action in actions) {
            when (action) {
                is Action.ToggleSector -> {
                    val sector = action.sector
                    if (sector in snapshot.selectedSectors) {
                        stopSector(sector)
                    } else {
                        startSector(sector)
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { jobs.values.forEach(Job::cancel) }
    }
    return snapshot
}
