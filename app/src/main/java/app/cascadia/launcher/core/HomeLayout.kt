package app.cascadia.launcher.core

/** Center the home group with a slight upper bias; keep it scrollable on short screens.
 * All arguments/results use the same units (dp in the UI).
 */
fun homeTopSpacing(viewport: Float, groupHeight: Float): Float {
    val upperLimit = (viewport * 0.28f).coerceAtLeast(25f)
    return ((viewport - groupHeight) / 2f).coerceIn(25f, upperLimit)
}
