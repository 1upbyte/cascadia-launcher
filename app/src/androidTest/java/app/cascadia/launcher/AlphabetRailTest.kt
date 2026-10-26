package app.cascadia.launcher

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.cascadia.launcher.ui.AlphabetRail
import app.cascadia.launcher.ui.CascadiaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AlphabetRailTest {
    @get:Rule val compose = createComposeRule()

    private fun showRail(onSelect: (String) -> Unit = {}) {
        compose.setContent {
            CascadiaTheme {
                Box(Modifier.fillMaxSize().testTag("rail-container")) {
                    AlphabetRail(
                        occupied = setOf("A", "M", "Z"), selected = null,
                        onSelect = onSelect, onReturnHome = {}, onRelease = {}, onAccessibleOpen = {},
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
            }
        }
    }

    @Test fun hidesUnusedLettersAndCentersCompactRail() {
        showRail()
        listOf("A", "M", "Z").forEach {
            compose.onNodeWithText(it, useUnmergedTree = true).assertIsDisplayed()
        }
        listOf("B", "N", "#").forEach {
            compose.onNodeWithText(it, useUnmergedTree = true).assertDoesNotExist()
        }
        val container = compose.onNodeWithTag("rail-container").fetchSemanticsNode().boundsInRoot
        val rail = compose.onNodeWithContentDescription("Alphabet and favorites shortcut").fetchSemanticsNode().boundsInRoot
        assertEquals(container.center.y, rail.center.y, 1f)
        assertTrue(rail.height < container.height / 2)
    }

    @Test fun touchAreaExtendsLeftOfVisibleLetters() {
        val selected = mutableListOf<String>()
        showRail { selected += it }
        compose.onNodeWithContentDescription("Alphabet and favorites shortcut").performTouchInput {
            // Start at the list-facing edge, not on the narrow visible glyph strip.
            down(Offset(1f, height * 1.5f / 4))
            moveTo(Offset(1f, height - 1f))
            up()
        }
        compose.runOnIdle {
            assertEquals(listOf("A", "Z"), selected.distinct())
        }
    }

    @Test fun previewFollowsFingerAndDisappearsOnRelease() {
        showRail()
        val rail = compose.onNodeWithContentDescription("Alphabet and favorites shortcut")
        rail.performTouchInput { down(Offset(center.x, center.y)) }
        compose.waitForIdle()
        val first = compose.onNodeWithTag("rail-letter-preview", useUnmergedTree = true)
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        rail.performTouchInput { moveTo(Offset(-120f, height - 1f)) }
        compose.waitForIdle()
        val pulled = compose.onNodeWithTag("rail-letter-preview", useUnmergedTree = true)
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue(pulled.left < first.left)
        assertTrue(pulled.top >= first.top)
        assertTrue(pulled.left >= 0f)
        rail.performTouchInput { up() }
        compose.waitForIdle()
        compose.onNodeWithTag("rail-letter-preview", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun dragUsesOnlyVisibleLettersInOrder() {
        val selected = mutableListOf<String>()
        showRail { selected += it }
        compose.onNodeWithContentDescription("Alphabet and favorites shortcut").performTouchInput {
            down(Offset(center.x, height * 1.5f / 4))
            moveTo(Offset(center.x, height * 2.5f / 4))
            moveTo(Offset(center.x, height - 1f))
            up()
        }
        compose.runOnIdle {
            assertEquals(listOf("A", "M", "Z"), selected.distinct())
        }
    }

    @Test fun railLeavesSystemBackEdgeAvailableAcrossLetterAndFavoritesGestures() {
        lateinit var view: View
        val selected = mutableListOf<String>()
        var homeReturns = 0
        compose.setContent {
            view = LocalView.current
            CascadiaTheme {
                Box(Modifier.fillMaxSize()) {
                    AlphabetRail(
                        occupied = setOf("A", "M", "Z"), selected = null,
                        onSelect = { selected += it },
                        onReturnHome = { homeReturns++ },
                        onRelease = {}, onAccessibleOpen = {},
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
            }
        }
        compose.runOnIdle { assertTrue(view.systemGestureExclusionRects.isEmpty()) }
        val rail = compose.onNodeWithContentDescription("Alphabet and favorites shortcut")
        rail.performTouchInput { down(Offset(center.x, height * 1.5f / 4)) }
        compose.runOnIdle { assertTrue(view.systemGestureExclusionRects.isEmpty()) }
        // Keeping Back available must preserve continuous rail scrubbing.
        rail.performTouchInput { moveTo(Offset(center.x, height - 1f)); up() }
        compose.runOnIdle {
            assertEquals(listOf("A", "Z"), selected.distinct())
            assertTrue(view.systemGestureExclusionRects.isEmpty())
        }
        rail.performTouchInput { click(Offset(center.x, 1f)) }
        compose.runOnIdle {
            assertEquals(1, homeReturns)
            assertTrue(view.systemGestureExclusionRects.isEmpty())
        }
    }
}
