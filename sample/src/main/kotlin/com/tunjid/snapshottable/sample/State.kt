package com.tunjid.snapshottable.sample

import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable

@Snapshottable
interface State {
    @SnapshotSpec
    data class Immutable(
        val selectedSectors: List<Sector>,
        val stockStates: List<StockState>,
    ) : State
}
