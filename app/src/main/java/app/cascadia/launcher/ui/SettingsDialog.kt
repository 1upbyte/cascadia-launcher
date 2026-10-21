package app.cascadia.launcher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import app.cascadia.launcher.core.IconShape
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
    iconShape: IconShape,
    onIconShape: (IconShape) -> Unit,
    isDefaultHome: Boolean,
    onChooseHome: () -> Unit,
    onWallpaper: () -> Unit,
    onScrim: (Float) -> Unit,
    onReorder: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    var localScrim by remember(scrim) { mutableFloatStateOf(scrim) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth().padding(20.dp).heightIn(max = 640.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
                Text("Make it yours", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onChooseHome) {
                    Text(if (isDefaultHome) "Default home settings" else "Set Cascadia as default home")
                }
                TextButton(onClick = onReorder) { Text("Reorder favorites") }
                TextButton(onClick = onWallpaper) { Text("Change system wallpaper") }
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
