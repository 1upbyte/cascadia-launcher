package app.cascadia.launcher.core

object RailSettings {
    const val DEFAULT_BOTTOM_MARGIN = 192f
    const val MIN_BOTTOM_MARGIN = 0f
    const val MAX_BOTTOM_MARGIN = 384f
    const val DEFAULT_SPACING = 22f
    const val MIN_SPACING = 16f
    const val MAX_SPACING = 32f

    fun normalizeBottomMargin(value: Float): Float =
        if (value.isFinite()) value.coerceIn(MIN_BOTTOM_MARGIN, MAX_BOTTOM_MARGIN) else DEFAULT_BOTTOM_MARGIN

    fun normalizeSpacing(value: Float): Float =
        if (value.isFinite()) value.coerceIn(MIN_SPACING, MAX_SPACING) else DEFAULT_SPACING
}
