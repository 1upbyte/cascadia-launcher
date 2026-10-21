package app.cascadia.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import app.cascadia.launcher.core.IconShape
import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun FooterAppRow(label: String, @DrawableRes icon: Int, iconShape: IconShape, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = LauncherLayout.RowMinHeight)
            .clickable(role = Role.Button, onClickLabel = "Open $label", onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(LauncherLayout.IconSize)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), iconShape.composeShape()),
            contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.width(LauncherLayout.IconLabelGap))
        Text(label, fontFamily = LauncherFont, fontSize = LauncherLayout.LabelSize,
            lineHeight = LauncherLayout.LabelLineHeight, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}
