package app.cascadia.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AppScaleTest {
    @Test fun corruptOrNonFiniteValuesFallBackToDefault() {
        listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach {
            assertEquals(1f, AppScale.normalize(it), 0f)
        }
    }
    @Test fun outOfRangeValuesAreClampedAndSavedValuesArePreserved() {
        assertEquals(0.75f, AppScale.normalize(-2f), 0f)
        assertEquals(1.5f, AppScale.normalize(10f), 0f)
        assertEquals(1.25f, AppScale.normalize(1.25f), 0f)
    }
}
