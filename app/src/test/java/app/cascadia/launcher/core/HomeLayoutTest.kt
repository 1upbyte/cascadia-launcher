package app.cascadia.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {
    @Test fun largerFavoritesGroupMovesUp() {
        assertTrue(homeTopSpacing(840f, 660f) < homeTopSpacing(840f, 400f))
    }
    @Test fun longFavoritesListKeepsSmallScrollableTopMargin() {
        assertEquals(25f, homeTopSpacing(600f, 1000f), 0f)
    }
    @Test fun sparseHomeHasUpperBiasRatherThanDroppingToBottom() {
        assertEquals(840f * 0.28f, homeTopSpacing(840f, 180f), 0.01f)
    }
    @Test fun fullHomeGroupIsCenteredWhenSpaceAllows() {
        assertEquals(90f, homeTopSpacing(840f, 660f), 0f)
    }
    @Test fun tinyViewportDoesNotProduceInvalidRange() {
        assertEquals(25f, homeTopSpacing(40f, 180f), 0f)
    }
}
