package app.cascadia.launcher

import android.content.Intent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cascadia.launcher.core.AppIndex
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherTest {
    @get:Rule val compose = createAndroidComposeRule<LauncherActivity>()
    private val application get() = compose.activity.application as CascadiaApplication

    @Before fun prepare() {
        runBlocking {
            application.preferences.preferences.first().pins.forEach { application.preferences.togglePin(it) }
        }
        compose.waitUntil(15_000) { !application.apps.inventory.value.loading }
        compose.waitForIdle()
    }

    private fun assertMode(mode: String) {
        compose.onNodeWithTag("app-list")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, mode))
    }

    private fun openApps() {
        compose.onNodeWithContentDescription("Alphabet. Browse all apps")
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.waitForIdle()
        assertMode("All apps")
    }

    @Test fun startsWithFavoritesOnlyAndBackClosesBrowser() {
        assertMode("Favorites")
        openApps()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertMode("Favorites")
    }

    @Test fun backgroundTapAndSwipeDoNotDismissBrowser() {
        openApps()
        compose.onNodeWithTag("app-list").performScrollToNode(hasTestTag("browse-tail"))
        compose.onNodeWithTag("browse-tail").performTouchInput {
            // Empty trailing background is a scrolling surface, not a dismiss target.
            click(Offset(width - 2f, height * 0.7f))
            swipe(start = Offset(width - 2f, height * 0.75f), end = Offset(width - 2f, height * 0.3f))
        }
        assertMode("All apps")
    }

    @Test fun railDragLeavesListVisibleOnRelease() {
        compose.onNodeWithContentDescription("Alphabet. Browse all apps").performTouchInput {
            down(Offset(center.x, height * 0.1f))
            moveTo(Offset(center.x, height * 0.85f))
            up()
        }
        compose.waitForIdle()
        assertMode("All apps")
    }

    @Test fun canPinAndUnpinAppWithLongPress() {
        val app = AppIndex.build(application.apps.inventory.value.apps).sections.first().apps.first()
        openApps()
        compose.onNodeWithText(app.label).performTouchInput { longClick() }
        compose.onNodeWithText("Pin to favorites").performClick()
        compose.waitUntil(5_000) { runBlocking { app.id in application.preferences.preferences.first().pins } }
        compose.waitForIdle()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText(app.label).assertIsDisplayed().performTouchInput { longClick() }
        compose.onNodeWithText("Unpin from favorites").performClick()
        compose.waitUntil(5_000) { runBlocking { app.id !in application.preferences.preferences.first().pins } }
    }

    @Test fun clockTapOpensExternalClockWithoutAlarmPermission() {
        val launcher = compose.activity
        org.junit.Assume.assumeTrue(
            launcher.packageManager.queryIntentActivities(
                Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS),
                android.content.pm.PackageManager.MATCH_DEFAULT_ONLY,
            ).isNotEmpty(),
        )
        compose.onNodeWithTag("home-clock").performClick()
        compose.waitUntil(10_000) {
            !launcher.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)
        }
        compose.runOnUiThread {
            launcher.startActivity(Intent(launcher, LauncherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        compose.waitUntil(10_000) {
            launcher.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)
        }
        compose.waitForIdle()
        assertMode("Favorites")
    }

    @Test fun browsingScrollSurvivesActivityRecreation() {
        val last = AppIndex.build(application.apps.inventory.value.apps).sections.last().apps.last()
        openApps()
        compose.onNodeWithTag("app-list").performScrollToNode(hasText(last.label))
        compose.onNodeWithText(last.label).assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        assertMode("All apps")
        compose.onNodeWithText(last.label).assertIsDisplayed()
    }

    @Test fun backOnFavoritesDoesNotExitLauncher() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertMode("Favorites")
        org.junit.Assert.assertFalse(compose.activity.isFinishing)
    }

    @Test fun reorderPersistsAcrossActivityRecreation() {
        val apps = AppIndex.build(application.apps.inventory.value.apps).sections.flatMap { it.apps }.take(2)
        org.junit.Assert.assertEquals(2, apps.size)
        runBlocking { apps.forEach { application.preferences.togglePin(it.id) } }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(apps[1].label).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Reorder favorites").performClick()
        compose.onAllNodesWithText("Up")[1].performClick()
        compose.waitUntil(5_000) {
            runBlocking { application.preferences.preferences.first().pins.firstOrNull() == apps[1].id }
        }
        compose.onNodeWithText("Done").performClick()
        compose.waitForIdle()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        assertMode("Favorites")
        org.junit.Assert.assertEquals(
            listOf(apps[1].id, apps[0].id),
            runBlocking { application.preferences.preferences.first().pins },
        )
    }

    @Test fun homeIntentClosesBrowser() {
        openApps()
        compose.runOnUiThread {
            // Deliver to the existing activity without replacing ActivityScenario\'s task.
            InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(
                compose.activity,
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            )
        }
        compose.waitUntil(5_000) {
            compose.onAllNodes(
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Favorites"),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        assertMode("Favorites")
    }
}
