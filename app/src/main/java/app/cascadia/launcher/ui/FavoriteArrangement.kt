package app.cascadia.launcher.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.cascadia.launcher.core.AppEntry

/** Gesture-local order avoids disk writes and preference round trips during dragging. */
@Stable
internal class FavoriteArrangement(
    private val list: LazyListState,
    private val save: (List<String>) -> Unit,
    private val pickup: () -> Unit,
) {
    var ordered by mutableStateOf<List<AppEntry>>(emptyList())
        private set
    var draggedId by mutableStateOf<String?>(null)
        private set
    private var center by mutableFloatStateOf(0f)
    private var original = emptyList<AppEntry>()
    private var enabled = false

    fun sync(pins: List<AppEntry>, arranging: Boolean) {
        if (!arranging || !enabled) {
            ordered = pins
            draggedId = null
        } else {
            val current = pins.associateBy { it.id }
            val known = ordered.map { it.id }.toSet()
            ordered = ordered.mapNotNull { current[it.id] } + pins.filter { it.id !in known }
            if (draggedId != null && draggedId !in current) draggedId = null
        }
        enabled = arranging
    }

    fun start(id: String) {
        if (!enabled) return
        val item = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "pin:$id" } ?: return
        original = ordered
        center = item.offset + item.size / 2f
        draggedId = id
        pickup()
    }

    fun dragBy(delta: Float) {
        if (draggedId == null) return
        val info = list.layoutInfo
        center = (center + delta).coerceIn(info.viewportStartOffset.toFloat(), info.viewportEndOffset.toFloat())
        reorderAtCenter()
    }

    fun translation(id: String): Float {
        if (draggedId != id) return 0f
        val item = list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "pin:$id" } ?: return 0f
        return center - item.offset - item.size / 2f
    }

    private fun reorderAtCenter() {
        val id = draggedId ?: return
        val from = ordered.indexOfFirst { it.id == id }
        if (from < 0) return
        val positions = ordered.mapIndexed { index, app -> "pin:${app.id}" to index }.toMap()
        val crossed = list.layoutInfo.visibleItemsInfo.mapNotNull { item ->
            val position = positions[item.key] ?: return@mapNotNull null
            val midpoint = item.offset + item.size / 2f
            position.takeIf { (position > from && center > midpoint) || (position < from && center < midpoint) }
        }
        val to = crossed.maxByOrNull { kotlin.math.abs(it - from) } ?: return
        move(from, to)
    }

    private fun move(from: Int, to: Int) {
        // Preserve the viewport when the first visible favorite changes its index.
        list.requestScrollToItem(list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset)
        ordered = ordered.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun moveBy(id: String, delta: Int) {
        if (!enabled || draggedId != null) return
        val from = ordered.indexOfFirst { it.id == id }
        if (from < 0) return
        val to = (from + delta).coerceIn(ordered.indices)
        if (from != to) {
            move(from, to)
            save(ordered.map { it.id })
        }
    }

    fun finish(cancel: Boolean = false) {
        if (draggedId == null) return
        if (cancel) {
            val current = ordered.associateBy { it.id }
            val known = original.map { it.id }.toSet()
            ordered = original.mapNotNull { current[it.id] } + ordered.filter { it.id !in known }
        } else {
            save(ordered.map { it.id })
        }
        draggedId = null
        original = emptyList()
    }

    suspend fun scrollAtEdge(edge: Float, maxSpeed: Float, seconds: Float) {
        if (draggedId == null) return
        val info = list.layoutInfo
        val top = info.viewportStartOffset + edge
        val bottom = info.viewportEndOffset - edge
        val speed = when {
            center < top -> -((top - center) / edge).coerceIn(0f, 1f)
            center > bottom -> ((center - bottom) / edge).coerceIn(0f, 1f)
            else -> 0f
        }
        if (speed != 0f) {
            list.scrollBy(speed * maxSpeed * seconds)
            reorderAtCenter()
        }
    }
}

@Composable
internal fun FavoriteDragHandle(app: AppEntry, arrangement: FavoriteArrangement) {
    var menu by remember { mutableStateOf(false) }
    val position = arrangement.ordered.indexOfFirst { it.id == app.id }
    Box {
        IconButton(
            onClick = { menu = true },
            modifier = Modifier.size(48.dp).testTag("favorite-handle:${app.id}")
                .semantics {
                    contentDescription = "Reorder ${app.label}"
                    customActions = buildList {
                        if (position > 0) add(CustomAccessibilityAction("Move up") { arrangement.moveBy(app.id, -1); true })
                        if (position < arrangement.ordered.lastIndex) {
                            add(CustomAccessibilityAction("Move down") { arrangement.moveBy(app.id, 1); true })
                        }
                    }
                }
                .pointerInput(app.id, arrangement) {
                    detectDragGestures(
                        onDragStart = { arrangement.start(app.id) },
                        onDragEnd = { arrangement.finish() },
                        onDragCancel = { arrangement.finish(cancel = true) },
                    ) { change, amount ->
                        change.consume()
                        arrangement.dragBy(amount.y)
                    }
                },
        ) { Text("≡") }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Move up") }, enabled = position > 0,
                onClick = { arrangement.moveBy(app.id, -1); menu = false })
            DropdownMenuItem(text = { Text("Move down") }, enabled = position < arrangement.ordered.lastIndex,
                onClick = { arrangement.moveBy(app.id, 1); menu = false })
        }
    }
}
