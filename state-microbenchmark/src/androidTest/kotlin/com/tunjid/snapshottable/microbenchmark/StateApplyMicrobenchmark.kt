package com.tunjid.snapshottable.microbenchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Microbenchmark isolating the per-update *state-apply* cost of the two state
 * implementations, with no Compose UI involved. This is the comparison the
 * macrobenchmark could not resolve: there, the dominant cost was row recomposition
 * (identical between modes), which swamped the cheap state-apply difference.
 *
 * Each measured "apply" mirrors what the sample's StateProduction does when a fresh
 * price list arrives for one sector:
 *  - Immutable: rebuild [State] via copy(), which rebuilds the stockStates list
 *    (length [holderCount]) and replaces one entry. Work grows with [holderCount].
 *  - SnapshotMutable: write the changed holder's stocks in place. Work is O(1),
 *    independent of [holderCount].
 *
 * Beyond time, microbenchmark reports allocationCount, which is deterministic and
 * device-independent: copy() churns objects every tick; the snapshot write does not.
 *
 * The two snapshot-write values alternate between [freshA] and [freshB] so the write
 * is never skipped by the snapshot state's structural-equality policy.
 */
@RunWith(Parameterized::class)
class StateApplyMicrobenchmark(private val holderCount: Int) {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val sectors: List<Sector> =
        List(holderCount) { Sector.entries[it % Sector.entries.size] }

    private val freshA: List<Stock> =
        List(STOCKS_PER_HOLDER) { Stock(ticker = "TKR$it", name = "Stock $it", price = it.toDouble()) }
    private val freshB: List<Stock> =
        List(STOCKS_PER_HOLDER) { Stock(ticker = "TKR$it", name = "Stock $it", price = it + 1.0) }

    @Test
    fun immutableApply() {
        var state = State.Immutable(
            selectedSectors = sectors,
            stockStates = sectors.map { StockState.Immutable(sector = it, stocks = freshA) },
        )
        benchmarkRule.measureRepeated {
            // Replace exactly one holder, mirroring "one sector got new prices".
            state = state.copy(
                stockStates = state.stockStates.mapIndexed { index, holder ->
                    if (index == 0) StockState.Immutable(sector = holder.sector, stocks = freshA)
                    else holder
                },
            )
        }
    }

    @Test
    fun snapshotMutableApply() {
        val holders = sectors.map { StockState.Immutable(sector = it, stocks = freshA).toSnapshotMutable() }
        val target = holders.first()
        val state = State.Immutable(selectedSectors = emptyList(), stockStates = emptyList())
            .toSnapshotMutable()
        state.selectedSectors = sectors
        state.stockStates = holders
        var flip = false
        benchmarkRule.measureRepeated {
            // Write the changed holder in place; independent of holderCount.
            target.stocks = if (flip) freshA else freshB
            flip = !flip
        }
    }

    companion object {
        private const val STOCKS_PER_HOLDER = 60

        @JvmStatic
        @Parameterized.Parameters(name = "holders={0}")
        fun holderCounts(): List<Int> = listOf(2, 16, 64)
    }
}
