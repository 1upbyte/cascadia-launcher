package app.cascadia.launcher.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Run separately from timing tests; copy the resulting profile into the app. */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val profile = BaselineProfileRule()

    @Test fun generate() = profile.collect(packageName = "app.cascadia.launcher") {
        pressHome()
        startActivityAndWait()
        check(device.wait(Until.hasObject(By.desc("Alphabet. Browse all apps")), 5_000))
        val bounds = device.findObject(By.desc("Alphabet. Browse all apps")).visibleBounds
        device.swipe(bounds.centerX(), bounds.top + 4, bounds.centerX(), bounds.bottom - 4, 40)
        device.swipe(bounds.centerX(), bounds.bottom - 4, bounds.centerX(), bounds.top + 4, 40)
        device.pressBack()
        device.waitForIdle()
    }
}
