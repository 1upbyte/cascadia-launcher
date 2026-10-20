package app.cascadia.launcher.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import app.cascadia.launcher.R

/** Bundled OFL font; no provider, runtime download, or proprietary font dependency. */
@OptIn(ExperimentalTextApi::class)
val LauncherFont = FontFamily(
    Font(R.font.nunito_sans_variable, weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.nunito_sans_variable, weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.nunito_sans_variable, weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.nunito_sans_variable, weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

val LauncherTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = LauncherFont),
        displayMedium = base.displayMedium.copy(fontFamily = LauncherFont),
        displaySmall = base.displaySmall.copy(fontFamily = LauncherFont),
        headlineLarge = base.headlineLarge.copy(fontFamily = LauncherFont),
        headlineMedium = base.headlineMedium.copy(fontFamily = LauncherFont),
        headlineSmall = base.headlineSmall.copy(fontFamily = LauncherFont),
        titleLarge = base.titleLarge.copy(fontFamily = LauncherFont),
        titleMedium = base.titleMedium.copy(fontFamily = LauncherFont),
        titleSmall = base.titleSmall.copy(fontFamily = LauncherFont),
        bodyLarge = base.bodyLarge.copy(fontFamily = LauncherFont),
        bodyMedium = base.bodyMedium.copy(fontFamily = LauncherFont),
        bodySmall = base.bodySmall.copy(fontFamily = LauncherFont),
        labelLarge = base.labelLarge.copy(fontFamily = LauncherFont),
        labelMedium = base.labelMedium.copy(fontFamily = LauncherFont),
        labelSmall = base.labelSmall.copy(fontFamily = LauncherFont),
    )
}
