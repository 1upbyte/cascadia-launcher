package app.cascadia.launcher.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import app.cascadia.launcher.core.IconShape

/** Continuous-curvature squircle, rather than straight edges joined by circular corners. */
private object SquircleShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        return Outline.Generic(Path().apply {
            moveTo(w * 0.5f, 0f)
            cubicTo(w * 0.1f, 0f, 0f, h * 0.1f, 0f, h * 0.5f)
            cubicTo(0f, h * 0.9f, w * 0.1f, h, w * 0.5f, h)
            cubicTo(w * 0.9f, h, w, h * 0.9f, w, h * 0.5f)
            cubicTo(w, h * 0.1f, w * 0.9f, 0f, w * 0.5f, 0f)
            close()
        })
    }
}

fun IconShape.composeShape(): Shape = when (this) {
    IconShape.SQUARE -> RectangleShape
    IconShape.SQUIRCLE -> SquircleShape
    IconShape.CIRCLE -> CircleShape
}
