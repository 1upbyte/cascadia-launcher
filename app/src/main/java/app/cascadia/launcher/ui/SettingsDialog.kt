package app.cascadia.launcher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import app.cascadia.launcher.core.IconShape
import app.cascadia.launcher.core.AppScale
import app.cascadia.launcher.core.RailSettings
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.roundToInt
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun SettingsDialog(
    scrim: Float,
    appScale: Float,
    onAppScale: (Float) -> Unit,
    onAppScaleFinished: (Float) -> Unit,
    railBottomMargin: Float,
    onRailBottomMargin: (Float) -> Unit,
    onRailBottomMarginFinished: (Float) -> Unit,
    railSpacing: Float,
    onRailSpacing: (Float) -> Unit,
    onRailSpacingFinished: (Float) -> Unit,
    iconShape: IconShape,
    onIconShape: (IconShape) -> Unit,
    isDefaultHome: Boolean,
    onChooseHome: () -> Unit,
    onWallpaper: () -> Unit,
    onScrim: (Float) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    var sliderScale by remember(appScale) { mutableFloatStateOf(appScale) }
    var localScrim by remember(scrim) { mutableFloatStateOf(scrim) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth().padding(20.dp).heightIn(max = 640.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
                Text("Make it yours", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onChooseHome) {
                    Text(if (isDefaultHome) "Default home settings" else "Set Cascadia as default home")
                }
                TextButton(onClick = onWallpaper) { Text("Change system wallpaper") }
                Spacer(Modifier.height(16.dp))
                Text("App scale · ${(appScale * 100).roundToInt()}%", style = MaterialTheme.typography.titleMedium)
                Slider(value = sliderScale, onValueChange = { sliderScale = it; onAppScale(it) },
                    onValueChangeFinished = { onAppScaleFinished(sliderScale) },
                    valueRange = AppScale.MIN..AppScale.MAX, steps = 14,
                    modifier = Modifier.testTag("app-scale-slider").semantics { contentDescription = "App scale" })
                Text("Resize app icons, labels, and spacing.", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { sliderScale = 1f; onAppScale(1f); onAppScaleFinished(1f) }) { Text("Reset to 100%") }
                Spacer(Modifier.height(16.dp))
                Text("Alphabet rail", style = MaterialTheme.typography.titleMedium)
                RailSlider(label = "Starting height", value = railBottomMargin, unit = "dp",
                    range = RailSettings.MIN_BOTTOM_MARGIN..RailSettings.MAX_BOTTOM_MARGIN, steps = 47,
                    tag = "rail-height-slider", onChange = onRailBottomMargin, onFinished = onRailBottomMarginFinished)
                Text("Distance above the bottom edge. Increase to move the rail up.", style = MaterialTheme.typography.bodySmall)
                RailSlider(label = "Letter spacing", value = railSpacing, unit = "sp",
                    range = RailSettings.MIN_SPACING..RailSettings.MAX_SPACING, steps = 15,
                    tag = "rail-spacing-slider", onChange = onRailSpacing, onFinished = onRailSpacingFinished)
                TextButton(onClick = {
                    onRailBottomMargin(RailSettings.DEFAULT_BOTTOM_MARGIN)
                    onRailBottomMarginFinished(RailSettings.DEFAULT_BOTTOM_MARGIN)
                    onRailSpacing(RailSettings.DEFAULT_SPACING)
                    onRailSpacingFinished(RailSettings.DEFAULT_SPACING)
                }) { Text("Reset rail to defaults") }
                Spacer(Modifier.height(16.dp))
                Text("Icon shape", style = MaterialTheme.typography.titleMedium)
                Column(Modifier.selectableGroup()) {
                    IconShape.entries.forEach { shape ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .selectable(selected = iconShape == shape, role = Role.RadioButton,
                                onClick = { onIconShape(shape) }),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = iconShape == shape, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(shape.label)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Wallpaper dimming")
                Slider(value = localScrim, onValueChange = { localScrim = it },
                    onValueChangeFinished = { onScrim(localScrim) }, valueRange = 0f..0.8f)
                Text("A darker backdrop keeps app names readable.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onRefresh) { Text("Refresh installed apps") }
                Spacer(Modifier.height(16.dp))
                Text("Quiet by design", style = MaterialTheme.typography.titleMedium)
                Text("No network permissions. No accounts, ads, analytics, or Google Play services. Preferences stay on this device; Android backup is disabled.",
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Text("Cascadia 0.1.0 · Android 10+", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
        }
    }
}

@Composable
private fun RailSlider(
    label: String,
    value: Float,
    unit: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    tag: String,
    onChange: (Float) -> Unit,
    onFinished: (Float) -> Unit,
) {
    var localValue by remember(value) { mutableFloatStateOf(value) }
    Text("$label · ${value.roundToInt()} $unit")
    Slider(value = localValue, onValueChange = { localValue = it; onChange(it) },
        onValueChangeFinished = { onFinished(localValue) }, valueRange = range, steps = steps,
        modifier = Modifier.testTag(tag).semantics { contentDescription = label })
}
