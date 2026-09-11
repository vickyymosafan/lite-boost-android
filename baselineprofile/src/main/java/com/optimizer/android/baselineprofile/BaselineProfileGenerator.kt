package com.optimizer.android.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(
            packageName = "com.optimizer.android",
            maxIterations = 15,
            stableIterations = 3
        ) {
            startActivityAndWait()
            device.findObject(By.scrollable(true))?.fling(Direction.DOWN)
            Thread.sleep(500)
            device.findObject(By.scrollable(true))?.fling(Direction.UP)
        }
    }
}
