package app.cascadia.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import app.cascadia.launcher.core.IconShape
import app.cascadia.launcher.core.AppScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import app.cascadia.launcher.core.AppEntry
import app.cascadia.launcher.data.AppRepository

@Composable
fun AppRow(
    app: AppEntry,
    repository: AppRepository,
    revision: Int,
    iconShape: IconShape,
    layout: AppLayout,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val pixels = with(LocalDensity.current) { (LauncherLayout.IconSize * AppScale.MAX).roundToPx() }
    val cached = remember(app.id, revision, pixels) {
        repository.cachedIcon(app.id, pixels, revision)?.asImageBitmap()
    }
    val icon by produceState<ImageBitmap?>(cached, app.id, revision, pixels) {
        value = repository.icon(app.id, pixels, revision)?.asImageBitmap()
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = layout.rowMinHeight)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = "Open ${app.label}",
                onLongClickLabel = "Options for ${app.label}",
                onClick = onClick, onLongClick = onLongClick,
            ).padding(vertical = layout.rowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(layout.iconSize).clip(iconShape.composeShape())
            .background(if (icon != null) Color.White else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))) {
            if (icon != null) Image(icon!!, contentDescription = null, Modifier.matchParentSize())
        }
        Spacer(Modifier.width(layout.iconLabelGap))
        Text(app.label, fontFamily = LauncherFont,
            fontSize = layout.labelSize, lineHeight = layout.labelLineHeight,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp, modifier = Modifier.weight(1f))
    }
}
