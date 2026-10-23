package app.cascadia.launcher.core

object AppScale {
    const val MIN = 0.75f
    const val MAX = 1.5f
    fun normalize(value: Float): Float = if (value.isFinite()) value.coerceIn(MIN, MAX) else 1f
}
