package app.cascadia.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.cascadia.launcher.core.AppEntry

@Composable
fun ReorderDialog(
    pins: List<AppEntry>,
    onMove: (String, Int) -> Unit,
    onUnpin: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // Optimistic local order prevents disk latency from slowing down dragging.
    var ordered by remember { mutableStateOf(pins) }
    var dragging by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pins) { if (dragging == null) ordered = pins }
    val move by rememberUpdatedState(onMove)
    val threshold = with(LocalDensity.current) { 112.dp.toPx() }

    fun moveLocally(id: String, delta: Int) {
        val from = ordered.indexOfFirst { it.id == id }
        if (from < 0) return
        val to = (from + delta).coerceIn(ordered.indices)
        if (to == from) return
        ordered = ordered.toMutableList().apply { add(to, removeAt(from)) }
        move(id, delta)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(16.dp), shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(20.dp)) {
                Text("Your favorites", style = MaterialTheme.typography.headlineMedium)
                Text("Drag a handle to reorder, or use the move buttons.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                LazyColumn(Modifier.weight(1f)) {
                    if (ordered.isEmpty()) item { Text("No favorites yet. Long-press an app in the alphabet list to pin it.") }
                    itemsIndexed(ordered, key = { _, app -> app.id }) { position, app ->
                        Column(Modifier.fillMaxWidth().background(
                            if (dragging == app.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surface,
                        ).padding(vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("≡", style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.size(48.dp).wrapContentSize()
                                        .semantics {
                                            contentDescription = "Drag to reorder ${app.label}"
                                            customActions = listOf(
                                                CustomAccessibilityAction("Move up") { moveLocally(app.id, -1); true },
                                                CustomAccessibilityAction("Move down") { moveLocally(app.id, 1); true },
                                            )
                                        }
                                        .pointerInput(app.id) {
                                            var distance = 0f
                                            detectDragGestures(
                                                onDragStart = { dragging = app.id; distance = 0f },
                                                onDragEnd = { dragging = null },
                                                onDragCancel = { dragging = null },
                                            ) { change, amount ->
                                                change.consume()
                                                distance += amount.y
                                                while (distance >= threshold) {
                                                    moveLocally(app.id, 1); distance -= threshold
                                                }
                                                while (distance <= -threshold) {
                                                    moveLocally(app.id, -1); distance += threshold
                                                }
                                            }
                                        })
                                Text(app.label, modifier = Modifier.weight(1f))
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { moveLocally(app.id, -1) }, enabled = position > 0) { Text("Up") }
                                TextButton(onClick = { moveLocally(app.id, 1) }, enabled = position < ordered.lastIndex) { Text("Down") }
                                TextButton(onClick = {
                                    ordered = ordered.filterNot { it.id == app.id }
                                    onUnpin(app.id)
                                }) { Text("Unpin") }
                            }
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
        }
    }
}
