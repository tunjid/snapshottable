package com.tunjid.snapshottable.macrobenchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TARGET_PACKAGE = "com.tunjid.snapshottable.sample"
private const val ITERATIONS = 5
private const val SECTORS_TO_EXERCISE = 3

@RunWith(AndroidJUnit4::class)
class StockBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun snapshotMutableScrollAndPager() = runScenario("SnapshotMutable")

    @Test
    fun immutableScrollAndPager() = runScenario("Immutable")

    private fun runScenario(mode: String) = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric(), StartupTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.COLD,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait { intent -> intent.putExtra("MODE", mode) }

        repeat(SECTORS_TO_EXERCISE) {
            val toggle = device.wait(
                Until.findObject(By.descContains("toggle_")),
                5_000,
            )
            toggle?.click()
            device.waitForIdle()

            val list = device.wait(
                Until.findObject(By.descContains("stockList_")),
                5_000,
            )
            list?.setGestureMargin(device.displayWidth / 5)
            repeat(4) {
                list?.fling(Direction.DOWN)
                device.waitForIdle()
            }
            repeat(2) { list?.fling(Direction.UP) }

            device.findObject(By.desc("sectorPager"))?.fling(Direction.RIGHT)
            device.waitForIdle()
        }
    }
}
