package app.cascadia.launcher.core

import kotlin.math.exp

/** Smooth spatial bell, evaluated at the actual finger position, not a snapped slot. */
fun railCurveWeight(distance: Float, spread: Float = 1.2f): Float {
    require(spread > 0f && spread.isFinite())
    val normalized = distance / spread
    return exp(-0.5f * normalized * normalized)
}

/** A longer rail/deeper pull bends more neighbors; values are in the same units. */
fun railCurveSpread(railHeight: Float, pull: Float): Float =
    (railHeight * 0.15f + pull * 0.2f).coerceAtLeast(1f)
