package app.cascadia.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeLayoutTest {
    @Test fun homeHeaderHasFixedUpperBias() {
        assertEquals(235.2f, homeTopSpacing(840f), 0.01f)
    }
    @Test fun tinyViewportDoesNotProduceInvalidRange() {
        assertEquals(25f, homeTopSpacing(40f), 0f)
    }
}
