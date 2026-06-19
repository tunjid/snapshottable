package com.tunjid.snapshottable.sample

import com.tunjid.snapshottable.SnapshotSpec
import com.tunjid.snapshottable.Snapshottable

@Snapshottable
interface StockState {
    @SnapshotSpec
    data class Immutable(
        val sector: Sector,
        val stocks: List<Stock>,
    ) : StockState
}
