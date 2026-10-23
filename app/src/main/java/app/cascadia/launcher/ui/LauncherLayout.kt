package app.cascadia.launcher.ui

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared home/browse geometry so icons, text and clock align consistently. */
object LauncherLayout {
    val ContentStart = 28.dp
    val RailTouchWidth = 68.dp
    val IconSize = 48.dp
    val IconLabelGap = 20.dp
    val RowMinHeight = 64.dp
    val LabelSize = 20.sp
    val LabelLineHeight = 28.sp
    val ClockSize = 48.sp
    val ClockLineHeight = 58.sp
    val DateSize = 16.sp
    val DateLineHeight = 24.sp
    val ClockFavoritesGap = 24.dp
}

/** Shared scaled dimensions keep list rows and rail-release geometry in agreement. */
data class AppLayout(val scale: Float) {
    val iconSize = LauncherLayout.IconSize * scale
    val iconLabelGap = LauncherLayout.IconLabelGap * scale
    val rowMinHeight = (LauncherLayout.RowMinHeight * scale).coerceAtLeast(48.dp)
    val rowPadding = 8.dp * scale
    val labelSize = LauncherLayout.LabelSize * scale
    val labelLineHeight = LauncherLayout.LabelLineHeight * scale
}
