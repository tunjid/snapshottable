package com.tunjid.snapshottable.microbenchmark

import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable

/**
 * Representative @Snapshottable fixture mirroring the :sample app's state shape, so
 * this self-instrumenting library module can microbenchmark the same copy-vs-snapshot
 * codegen without depending on the application module (whose generated code is not
 * visible to a separate module).
 */

enum class Sector { TECH, FINANCE, AGRICULTURE, ENERGY, HEALTHCARE }

data class Stock(
    val ticker: String,
    val name: String,
    val price: Double,
)

@Snapshottable
interface State {
    @SnapshotSpec
    data class Immutable(
        val selectedSectors: List<Sector>,
        val stockStates: List<StockState>,
    ) : State
}

@Snapshottable
interface StockState {
    @SnapshotSpec
    data class Immutable(
        val sector: Sector,
        val stocks: List<Stock>,
    ) : StockState
}
