package app.cascadia.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cascadia.launcher.core.AppIndex

@Composable
fun AlphabetRail(
    occupied: Set<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    onRelease: () -> Unit,
    onAccessibleOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val select by rememberUpdatedState(onSelect)
    val release by rememberUpdatedState(onRelease)
    Column(
        modifier.width(48.dp).fillMaxHeight()
            .background(Color.Black.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            .semantics(mergeDescendants = true) {
                contentDescription = "Alphabet. Browse all apps"
                onClick(label = "Open all apps") { onAccessibleOpen(); true }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    fun choose(y: Float) {
                        val slot = (y / size.height * AppIndex.RAIL.size).toInt()
                            .coerceIn(AppIndex.RAIL.indices)
                        select(AppIndex.RAIL[slot])
                    }
                    choose(down.position.y)
                    try {
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.pressed) choose(change.position.y)
                            change.consume()
                        } while (event.changes.any { it.id == down.id && it.pressed })
                    } finally { release() }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIndex.RAIL.forEach { letter ->
            Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    letter,
                    color = when {
                        selected == letter -> MaterialTheme.colorScheme.primary
                        letter in occupied -> Color.White
                        else -> Color.White.copy(alpha = 0.35f)
                    },
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}
