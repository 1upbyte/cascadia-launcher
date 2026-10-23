package app.cascadia.launcher.core

/** Keep the home header fixed as favorites grow downward; overflow scrolls.
 * All arguments/results use the same units (dp in the UI).
 */
fun homeTopSpacing(viewport: Float): Float = (viewport * 0.28f).coerceAtLeast(25f)
