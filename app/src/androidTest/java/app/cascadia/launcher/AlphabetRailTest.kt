package app.cascadia.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
                        onSelect = onSelect, onRelease = {}, onAccessibleOpen = {},
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
        val rail = compose.onNodeWithContentDescription("Alphabet. Browse all apps").fetchSemanticsNode().boundsInRoot
        assertEquals(container.center.y, rail.center.y, 1f)
        assertTrue(rail.height < container.height / 2)
    }

    @Test fun touchAreaExtendsLeftOfVisibleLetters() {
        val selected = mutableListOf<String>()
        showRail { selected += it }
        compose.onNodeWithContentDescription("Alphabet. Browse all apps").performTouchInput {
            // Start at the list-facing edge, not on the narrow visible glyph strip.
            down(Offset(1f, 1f))
            moveTo(Offset(1f, height - 1f))
            up()
        }
        compose.runOnIdle {
            assertEquals(listOf("A", "Z"), selected.distinct())
        }
    }

    @Test fun dragUsesOnlyVisibleLettersInOrder() {
        val selected = mutableListOf<String>()
        showRail { selected += it }
        compose.onNodeWithContentDescription("Alphabet. Browse all apps").performTouchInput {
            down(Offset(center.x, 1f))
            moveTo(Offset(center.x, center.y))
            moveTo(Offset(center.x, height - 1f))
            up()
        }
        compose.runOnIdle {
            assertEquals(listOf("A", "M", "Z"), selected.distinct())
        }
    }
}
