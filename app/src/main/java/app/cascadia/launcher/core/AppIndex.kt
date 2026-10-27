package app.cascadia.launcher.core

import java.text.Collator
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

data class AppEntry(val id: String, val label: String)

data class AppSection(val letter: String, val apps: List<AppEntry>)

data class AppIndex(val sections: List<AppSection>) {
    /** Absent letters resolve to the nearest occupied rail slot; ties go earlier. */
    fun nearestSection(letter: String): AppSection? {
        val position = RAIL.indexOf(letter).coerceAtLeast(0)
        return sections.minByOrNull { abs(RAIL.indexOf(it.letter) - position) }
    }

    companion object {
        val RAIL = ('A'..'Z').map(Char::toString) + "#"

        fun build(apps: List<AppEntry>, locale: Locale = Locale.getDefault()): AppIndex {
            val collator = Collator.getInstance(locale)
            val comparator = Comparator<AppEntry> { left, right ->
                val compared = collator.compare(left.label, right.label)
                if (compared == 0) left.id.compareTo(right.id) else compared
            }
            val grouped = apps.distinctBy { it.id }.groupBy { sectionFor(it.label) }
            return AppIndex(RAIL.mapNotNull { letter ->
                grouped[letter]?.let { AppSection(letter, it.sortedWith(comparator)) }
            })
        }

        /** MVP uses a Latin rail; accents fold to A–Z and other scripts use #. */
        fun sectionFor(label: String): String {
            val normalized = Normalizer.normalize(label.trim(), Normalizer.Form.NFD)
                .uppercase(Locale.ROOT)
            val initial = normalized.firstOrNull()
            return if (initial != null && initial in 'A'..'Z') initial.toString() else "#"
        }
    }
}

fun visiblePins(ids: List<String>, apps: List<AppEntry>): List<AppEntry> {
    val byId = apps.associateBy { it.id }
    return ids.distinct().mapNotNull(byId::get)
}

fun movePin(ids: List<String>, id: String, delta: Int): List<String> {
    val from = ids.indexOf(id)
    if (from < 0 || ids.isEmpty()) return ids
    val to = (from + delta).coerceIn(ids.indices)
    return ids.toMutableList().apply { add(to, removeAt(from)) }
}

/** Replace visible slots while preserving unavailable pins and newly added favorites. */
fun reorderPins(ids: List<String>, ordered: List<String>): List<String> {
    val existing = ids.toSet()
    val replacements = ordered.distinct().filter { it in existing }
    val moved = replacements.toSet()
    val iterator = replacements.iterator()
    return ids.map { if (it in moved) iterator.next() else it }
}
