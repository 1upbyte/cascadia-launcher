package app.cascadia.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Test

class RailSettingsTest {
    @Test fun invalidSavedValuesUseCurrentLayoutDefaults() {
        listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach {
            assertEquals(192f, RailSettings.normalizeBottomMargin(it), 0f)
            assertEquals(22f, RailSettings.normalizeSpacing(it), 0f)
        }
    }

    @Test fun savedValuesArePreservedWithinSupportedRanges() {
        assertEquals(256f, RailSettings.normalizeBottomMargin(256f), 0f)
        assertEquals(28f, RailSettings.normalizeSpacing(28f), 0f)
        assertEquals(0f, RailSettings.normalizeBottomMargin(-8f), 0f)
        assertEquals(384f, RailSettings.normalizeBottomMargin(500f), 0f)
        assertEquals(16f, RailSettings.normalizeSpacing(0f), 0f)
        assertEquals(32f, RailSettings.normalizeSpacing(100f), 0f)
    }
}
