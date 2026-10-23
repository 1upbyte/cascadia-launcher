package app.cascadia.launcher.core

/** Only the held section is materialized during rail scrubbing. */
fun AppIndex.browseSections(heldLetter: String?): List<AppSection> =
    if (heldLetter == null) sections else listOfNotNull(nearestSection(heldLetter))

/** Fixed first-letter height; clamp to the natural bottom for the final sections. */
fun browseSectionTop(
    viewportHeight: Float,
    slotCount: Int,
    slotHeight: Float,
    remainingHeight: Float,
    bottomInset: Float? = null,
): Float {
    if (slotCount <= 0 || viewportHeight <= 0f) return 0f
    val railHeight = (slotHeight * slotCount).coerceAtMost(
        (viewportHeight - (bottomInset ?: 0f) - if (bottomInset == null) 32f else 16f).coerceAtLeast(0f),
    )
    val firstSlotCenter = if (bottomInset == null) {
        (viewportHeight - railHeight) / 2f + railHeight / slotCount / 2f
    } else {
        viewportHeight - bottomInset - railHeight + railHeight / slotCount / 2f
    }
    val preferredTop = (firstSlotCenter - 24f).coerceAtLeast(0f)
    return maxOf(preferredTop, viewportHeight - 24f - remainingHeight)
}

/** Header + pins + help precede browsing in the single scrolling surface. */
fun AppIndex.sectionRow(
    letter: String,
    pinCount: Int,
    holding: Boolean,
    includeHomePrefix: Boolean = true,
): Int? {
    val target = nearestSection(letter) ?: return null
    var row = if (includeHomePrefix) pinCount + 2 else 0
    if (!holding) {
        for (section in sections) {
            if (section.letter == target.letter) break
            row += 1 + section.apps.size
        }
    }
    return row
}
