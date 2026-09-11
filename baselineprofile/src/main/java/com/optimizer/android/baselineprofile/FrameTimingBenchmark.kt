package com.optimizer.android.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FrameTimingBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun scrollDashboard() = rule.measureRepeated(
        packageName = "com.optimizer.android",
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = 5
    ) {
        startActivityAndWait()
        repeat(3) {
            device.findObject(By.scrollable(true))?.fling(Direction.DOWN)
            Thread.sleep(300)
            device.findObject(By.scrollable(true))?.fling(Direction.UP)
            Thread.sleep(300)
        }
    }
}
