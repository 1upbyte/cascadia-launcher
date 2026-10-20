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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cascadia.launcher.core.AppEntry
import app.cascadia.launcher.data.AppRepository

@Composable
fun AppRow(
    app: AppEntry,
    repository: AppRepository,
    revision: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val pixels = with(LocalDensity.current) { 36.dp.roundToPx() }
    val icon by produceState<ImageBitmap?>(null, app.id, revision, pixels) {
        value = repository.icon(app.id, pixels, revision)?.asImageBitmap()
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = "Open ${app.label}",
                onLongClickLabel = "Options for ${app.label}",
                onClick = onClick, onLongClick = onLongClick,
            ).padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Image(icon!!, contentDescription = null, Modifier.size(36.dp))
        else Box(Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape))
        Spacer(Modifier.width(16.dp))
        Text(app.label, fontSize = 23.sp, lineHeight = 29.sp, modifier = Modifier.weight(1f))
    }
}
