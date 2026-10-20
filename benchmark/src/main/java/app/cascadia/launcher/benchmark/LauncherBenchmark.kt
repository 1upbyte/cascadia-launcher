package app.cascadia.launcher.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE = "app.cascadia.launcher"

@RunWith(AndroidJUnit4::class)
class LauncherBenchmark {
    @get:Rule val benchmark = MacrobenchmarkRule()

    @Test fun coldStartup() = startup(StartupMode.COLD)
    @Test fun warmStartup() = startup(StartupMode.WARM)

    private fun startup(mode: StartupMode) = benchmark.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = mode,
        iterations = 5,
        setupBlock = { pressHome() },
        measureBlock = { startActivityAndWait() },
    )

    @Test fun alphabetDrag() = benchmark.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            check(device.wait(Until.hasObject(By.desc("Alphabet. Browse all apps")), 5_000))
        },
        measureBlock = {
            val bounds = device.findObject(By.desc("Alphabet. Browse all apps")).visibleBounds
            device.swipe(bounds.centerX(), bounds.top + 4, bounds.centerX(), bounds.bottom - 4, 50)
            device.swipe(bounds.centerX(), bounds.bottom - 4, bounds.centerX(), bounds.top + 4, 50)
            device.waitForIdle()
        },
    )
}
