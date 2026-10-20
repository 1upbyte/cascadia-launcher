package app.cascadia.launcher.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun CascadiaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        typography = LauncherTypography,
        colorScheme = darkColorScheme(
            primary = Color(0xFFD0E7D7),
            onPrimary = Color(0xFF13231D),
            surface = Color(0xFF18221E),
            onSurface = Color(0xFFF2F5F0),
            background = Color.Transparent,
        ),
        content = {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) { content() }
        },
    )
}
