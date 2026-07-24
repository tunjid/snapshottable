package com.tunjid.snapshottable.macrobenchmark

import android.os.SystemClock
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TARGET_PACKAGE = "com.tunjid.snapshottable.sample"
private const val ITERATIONS = 10
private const val UPDATE_INTERVAL_MS = 16L
private const val DWELL_MS = 5_000L

/**
 * Focused A/B benchmark isolating the cost of *state-update propagation* between the
 * two state implementations.
 *
 * Unlike [StockBenchmark] (which scrolls and pages), this holds the screen still and
 * saturates the update pipeline with a new price tick every [UPDATE_INTERVAL_MS], so
 * the measured work is dominated by how each mode applies updates rather than by
 * fling rendering (which is identical between modes):
 *  - Immutable rebuilds `State` and its lists via `copy()` on every tick.
 *  - SnapshotMutable writes the changed property in place.
 *
 * Captured per mode:
 *  - [FrameTimingMetric]: frame duration / jank under the update storm.
 *  - [MemoryUsageMetric]: peak memory, a proxy for Immutable's allocation churn.
 *  - [TraceSectionMetric] "applyStockUpdate": count + total time of the per-tick
 *    state-apply path (instrumented in the sample's StateProduction).
 *  - [TraceSectionMetric] "StockRow": count + total time of row recompositions.
 */
@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class StockUpdateBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun snapshotMutableUpdateStorm() = runUpdateStorm("SnapshotMutable")

    @Test
    fun immutableUpdateStorm() = runUpdateStorm("Immutable")

    private fun runUpdateStorm(mode: String) = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(
            FrameTimingMetric(),
            MemoryUsageMetric(MemoryUsageMetric.Mode.Max),
            TraceSectionMetric("applyStockUpdate", TraceSectionMetric.Mode.Sum),
            TraceSectionMetric("StockRow", TraceSectionMetric.Mode.Sum),
        ),
        // Full AOT compilation removes JIT warmup variance so the two modes are
        // compared on equal footing. Startup is identical between modes, so no
        // StartupTimingMetric and a WARM (not COLD) launch.
        compilationMode = CompilationMode.Full(),
        startupMode = StartupMode.WARM,
        iterations = ITERATIONS,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait { intent ->
            intent.putExtra("MODE", mode)
            intent.putExtra("UPDATE_INTERVAL_MS", UPDATE_INTERVAL_MS)
        }

        // Two sectors are auto-selected on launch, so a stock list is present without
        // any interaction. Wait for it, then hold still and let updates stream: the
        // measured frames reflect update propagation, not scrolling.
        device.wait(Until.findObject(By.descContains("stockList_")), 5_000)
        SystemClock.sleep(DWELL_MS)
    }
}
