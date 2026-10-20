package app.cascadia.launcher.core

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class AppIndexTest {
    @Test fun emptyInventoryHasNoSections() {
        assertTrue(AppIndex.build(emptyList()).sections.isEmpty())
        assertNull(AppIndex.build(emptyList()).nearestSection("A"))
    }

    @Test fun groupsAndSortsLabels() {
        val index = AppIndex.build(listOf(
            AppEntry("z", "Zoom"), AppEntry("b", "browser"), AppEntry("a2", "Atlas"),
            AppEntry("a1", "Alarm"), AppEntry("num", "123"),
        ), Locale.US)
        assertEquals(listOf("A", "B", "Z", "#"), index.sections.map { it.letter })
        assertEquals(listOf("Alarm", "Atlas"), index.sections[0].apps.map { it.label })
    }

    @Test fun normalizesAccentsAndWhitespace() {
        assertEquals("E", AppIndex.sectionFor("  Éclair"))
        assertEquals("A", AppIndex.sectionFor("Ångström"))
        assertEquals("U", AppIndex.sectionFor("über"))
    }

    @Test fun nonLatinAndEmptyLabelsUseFallback() {
        listOf("", " ", "写真", "Камера", "🙂 App", "42").forEach {
            assertEquals("#", AppIndex.sectionFor(it))
        }
    }

    @Test fun largeInventoryPreservesAllEntriesAndSections() {
        val apps = (0 until 2_000).map { number ->
            val letter = ('A'.code + number % 26).toChar()
            AppEntry("package$number/Main", "$letter app ${2_000 - number}")
        } + listOf(AppEntry("other/Main", "写真"), AppEntry("number/Main", "123"))
        val index = AppIndex.build(apps, Locale.US)
        val indexed = index.sections.flatMap { it.apps }
        assertEquals(apps.size, indexed.size)
        assertEquals(apps.map { it.id }.toSet(), indexed.map { it.id }.toSet())
        index.sections.forEach { section ->
            assertTrue(section.apps.all { AppIndex.sectionFor(it.label) == section.letter })
        }
    }

    @Test fun duplicateLabelsUseStableComponentTieBreaker() {
        val index = AppIndex.build(listOf(AppEntry("b/Activity", "App"), AppEntry("a/Activity", "App")), Locale.US)
        assertEquals(listOf("a/Activity", "b/Activity"), index.sections.single().apps.map { it.id })
    }

    @Test fun duplicateComponentsAreRemoved() {
        val index = AppIndex.build(listOf(AppEntry("same", "App"), AppEntry("same", "App")))
        assertEquals(1, index.sections.single().apps.size)
    }

    @Test fun emptyRailSlotsSelectNearestWithEarlierTieBreak() {
        val index = AppIndex.build(listOf(AppEntry("a", "App"), AppEntry("e", "Email")))
        assertEquals("A", index.nearestSection("C")?.letter)
        assertEquals("E", index.nearestSection("D")?.letter)
        assertEquals("E", index.nearestSection("Z")?.letter)
    }

    @Test fun exactSectionWins() {
        val index = AppIndex.build(listOf(AppEntry("a", "App"), AppEntry("e", "Email")))
        assertEquals("E", index.nearestSection("E")?.letter)
    }

    @Test fun pinsFollowManualOrderNotAlphabeticalOrder() {
        val apps = listOf(AppEntry("a", "Alarm"), AppEntry("z", "Zoom"))
        assertEquals(listOf("z", "a"), visiblePins(listOf("z", "a"), apps).map { it.id })
    }

    @Test fun staleAndDuplicatePinsDoNotCreateBrokenRows() {
        assertEquals(listOf(AppEntry("a", "Alarm")),
            visiblePins(listOf("gone", "a", "a"), listOf(AppEntry("a", "Alarm"))))
    }

    @Test fun reorderMovesInBothDirections() {
        assertEquals(listOf("b", "a", "c"), movePin(listOf("a", "b", "c"), "a", 1))
        assertEquals(listOf("c", "a", "b"), movePin(listOf("a", "b", "c"), "c", -2))
    }

    @Test fun reorderClampsAndIgnoresMissingIds() {
        assertEquals(listOf("b", "a"), movePin(listOf("a", "b"), "a", 10))
        assertEquals(listOf("a", "b"), movePin(listOf("a", "b"), "a", -10))
        assertEquals(listOf("a"), movePin(listOf("a"), "missing", 1))
        assertEquals(emptyList<String>(), movePin(emptyList(), "missing", 1))
    }
}
