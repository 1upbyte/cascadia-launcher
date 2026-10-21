package app.cascadia.launcher

import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cascadia.launcher.data.renderIcon
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IconRenderingTest {
    @Test fun adaptiveIconCornersRemainAvailableForLauncherMask() {
        val drawable = AdaptiveIconDrawable(ColorDrawable(Color.RED), ColorDrawable(Color.TRANSPARENT))
        val bitmap = renderIcon(drawable, 48)
        // A pre-masked system icon loses these pixels and cannot become square/squircle.
        assertEquals(Color.RED, bitmap.getPixel(0, 0))
        assertEquals(Color.RED, bitmap.getPixel(47, 47))
        assertEquals(Color.RED, bitmap.getPixel(24, 24))
    }
}
