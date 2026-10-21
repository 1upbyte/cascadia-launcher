package app.cascadia.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cascadia.launcher.core.AppIndex
import app.cascadia.launcher.core.railCurveSpread
import app.cascadia.launcher.core.railCurveWeight

@Composable
fun AlphabetRail(
    occupied: Set<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    onRelease: () -> Unit,
    onAccessibleOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val letters = remember(occupied) { AppIndex.RAIL.filter { it in occupied } }
    if (letters.isEmpty()) return

    val slotHeight = with(LocalDensity.current) { 19.sp.toDp() }
    var dragging by remember { mutableStateOf(false) }
    var finger by remember { mutableStateOf(Offset.Zero) }
    var activeLetter by remember { mutableStateOf<String?>(null) }
    var railLeft by remember { mutableFloatStateOf(0f) }
    var railHeight by remember { mutableFloatStateOf(1f) }
    val curveStrength by animateFloatAsState(
        targetValue = if (dragging) 1f else 0f,
        animationSpec = tween(150),
        label = "alphabet-curve",
    )
    val select by rememberUpdatedState(onSelect)
    val release by rememberUpdatedState(onRelease)
    Box(
        // Share the app-list gutter: the invisible touch area meets the rows,
        // while the visible letters sit 14 dp from the edge.
        modifier.width(LauncherLayout.RailTouchWidth).height(slotHeight * letters.size)
            .onSizeChanged { railHeight = it.height.toFloat() }
            .onGloballyPositioned { railLeft = it.positionInRoot().x }
            .systemGestureExclusion()
            .semantics(mergeDescendants = true) {
                contentDescription = "Alphabet. Browse all apps"
                onClick(label = "Open all apps") { onAccessibleOpen(); true }
            }
            .pointerInput(letters) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    var lastSlot = -1
                    fun choose(position: Offset) {
                        finger = position
                        val slot = (position.y / size.height.coerceAtLeast(1) * letters.size).toInt()
                            .coerceIn(letters.indices)
                        if (slot != lastSlot) {
                            lastSlot = slot
                            activeLetter = letters[slot]
                            select(letters[slot])
                        }
                    }
                    dragging = true
                    choose(down.position)
                    try {
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.pressed) choose(change.position)
                            change.consume()
                        } while (event.changes.any { it.id == down.id && it.pressed })
                    } finally {
                        dragging = false
                        release()
                    }
                }
            },
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            letters.forEachIndexed { index, letter ->
                Box(
                    Modifier.weight(1f).fillMaxSize().graphicsLayer {
                        // Read fast-moving pointer/animation state in the drawing layer:
                        // dragging never remeasures the list or changes touch geometry.
                        val actualSlotHeight = railHeight / letters.size
                        val glyphCenterX = size.width - 14.dp.toPx()
                        val maxPull = minOf(280.dp.toPx(), railLeft + glyphCenterX - 96.dp.toPx())
                            .coerceAtLeast(64.dp.toPx())
                        val pull = (glyphCenterX - finger.x + 32.dp.toPx())
                            .coerceIn(64.dp.toPx(), maxPull)
                        val fingerY = finger.y.coerceIn(actualSlotHeight / 2f, railHeight - actualSlotHeight / 2f)
                        val distance = (index + 0.5f) * actualSlotHeight - fingerY
                        val weight = railCurveWeight(distance, railCurveSpread(railHeight, pull))
                        // Only bend horizontally: the original slots and glyph sizes stay
                        // stable. The peak follows the finger continuously between letters.
                        translationX = -pull * weight * curveStrength
                    },
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text(
                        letter,
                        color = Color.White.copy(alpha = if (selected == null || selected == letter) 1f else 0.85f),
                        fontFamily = LauncherFont,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        lineHeight = 19.sp,
                        letterSpacing = 0.sp,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(0f, 1f), 3f),
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(28.dp).padding(horizontal = 2.dp),
                    )
                }
            }
        }
        if (dragging) {
            Box(
                Modifier.size(48.dp).graphicsLayer {
                    val bubbleWidth = size.width
                    val railGlyphX = LauncherLayout.RailTouchWidth.toPx() - 14.dp.toPx()
                    val maxPull = minOf(280.dp.toPx(), railLeft + railGlyphX - 96.dp.toPx())
                        .coerceAtLeast(64.dp.toPx())
                    val pull = (railGlyphX - finger.x + 32.dp.toPx()).coerceIn(64.dp.toPx(), maxPull)
                    val desiredX = minOf(finger.x - bubbleWidth - 24.dp.toPx(),
                        railGlyphX - pull * curveStrength - bubbleWidth - 16.dp.toPx())
                    translationX = desiredX.coerceAtLeast(8.dp.toPx() - railLeft)
                    translationY = (finger.y - size.height / 2f)
                        .coerceIn(0f, (railHeight - size.height).coerceAtLeast(0f))
                    alpha = curveStrength
                }.background(Color.White, CircleShape)
                    .clearAndSetSemantics { testTag = "rail-letter-preview" },
                contentAlignment = Alignment.Center,
            ) {
                Text(activeLetter.orEmpty(), color = Color.Black, fontFamily = LauncherFont,
                    fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Normal)
            }
        }
    }
}
