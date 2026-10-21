package app.cascadia.launcher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Date

@Composable
fun HomeClock(onClick: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    DisposableEffect(context, lifecycle) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { now = System.currentTimeMillis() }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_LOCALE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) now = System.currentTimeMillis()
        }
        lifecycle.addObserver(observer)
        onDispose {
            context.unregisterReceiver(receiver)
            lifecycle.removeObserver(observer)
        }
    }
    val date = Date(now)
    val textStyle = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
    Column(Modifier.testTag("home-clock")
        .clickable(role = Role.Button, onClickLabel = "Open clock", onClick = onClick)
        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
        Text(DateFormat.getTimeFormat(context).format(date),
            fontSize = LauncherLayout.ClockSize, lineHeight = LauncherLayout.ClockLineHeight,
            letterSpacing = 0.sp, style = textStyle)
        Text(DateFormat.format(DateFormat.getBestDateTimePattern(java.util.Locale.getDefault(), "EEE d MMM"), date).toString(),
            fontSize = LauncherLayout.DateSize, lineHeight = LauncherLayout.DateLineHeight,
            letterSpacing = 0.sp, style = textStyle)
    }
}
