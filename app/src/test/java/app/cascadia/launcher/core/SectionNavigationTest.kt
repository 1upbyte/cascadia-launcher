package app.cascadia.launcher.core

import org.junit.Assert.*
import org.junit.Test

class SectionNavigationTest {
    private val index = AppIndex(listOf(
        AppSection("A", listOf(AppEntry("a", "Alpha"), AppEntry("a2", "Another"))),
        AppSection("M", listOf(AppEntry("m", "Maps"))),
        AppSection("Z", listOf(AppEntry("z", "Zebra"))),
    ))

    @Test fun categoriesWithContentBelowUseFirstLetterHeight() {
        assertEquals(158.5f, browseSectionTop(840f, 26, 19f, 2000f), 0f)
        assertEquals(158.5f, browseSectionTop(840f, 26, 19f, 900f), 0f)
    }
    @Test fun finalSectionsStopAtNaturalBottom() {
        assertEquals(616f, browseSectionTop(840f, 26, 19f, 200f), 0f)
        assertEquals(700f, browseSectionTop(840f, 26, 19f, 116f), 0f)
    }
    @Test fun oversizedSectionsKeepTheSameStartHeight() {
        assertEquals(158.5f, browseSectionTop(840f, 26, 19f, 4000f), 0f)
    }
    @Test fun placementHandlesEmptyViewport() {
        assertEquals(0f, browseSectionTop(0f, 0, 19f, 116f), 0f)
    }

    @Test fun holdingOnlyIncludesThatLettersApps() {
        assertEquals(listOf("M"), index.browseSections("M").map { it.letter })
        assertEquals(listOf("m"), index.browseSections("M").single().apps.map { it.id })
    }
    @Test fun releasingRestoresAllSections() {
        assertEquals(index.sections, index.browseSections(null))
    }
    @Test fun heldSectionAlwaysFollowsHomePrefix() {
        assertEquals(5, index.sectionRow("A", 3, true))
        assertEquals(5, index.sectionRow("Z", 3, true))
    }
    @Test fun releaseFindsSameSectionInFullList() {
        assertEquals(8, index.sectionRow("M", 3, false))
        assertEquals(10, index.sectionRow("Z", 3, false))
    }
    @Test fun emptyInventoryHasNoTarget() {
        assertNull(AppIndex(emptyList()).sectionRow("A", 0, true))
        assertTrue(AppIndex(emptyList()).browseSections("A").isEmpty())
    }
    @Test fun missingLetterUsesSameNearestSectionForDisplayAndJump() {
        assertEquals("M", index.browseSections("N").single().letter)
        assertEquals(index.sectionRow("M", 0, false), index.sectionRow("N", 0, false))
    }
}
