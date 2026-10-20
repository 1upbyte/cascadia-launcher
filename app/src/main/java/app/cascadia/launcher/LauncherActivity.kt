package app.cascadia.launcher

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cascadia.launcher.ui.CascadiaTheme
import app.cascadia.launcher.ui.LauncherScreen

class LauncherActivity : ComponentActivity() {
    private var homeRequest by mutableIntStateOf(0)
    private var defaultHome by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        setContent {
            CascadiaTheme {
                LauncherScreen(
                    homeRequest = homeRequest,
                    isDefaultHome = defaultHome,
                    onChooseHome = ::chooseHome,
                    onWallpaper = { open(Intent(Intent.ACTION_SET_WALLPAPER), "No wallpaper picker is installed.") },
                    onClock = ::openClock,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        defaultHome = getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homeRequest++
    }

    private fun openClock() {
        val alarms = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        // Some clocks guard SHOW_ALARMS with SET_ALARM, even for viewing.
        // Open the clock's public launcher activity instead of requesting that
        // unnecessary permission. Respect a default handler when one exists.
        val preferredPackage = packageManager.resolveActivity(alarms, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
        val launches = packageManager.queryIntentActivities(alarms, PackageManager.MATCH_DEFAULT_ONLY)
            .distinctBy { it.activityInfo.packageName }
            .mapNotNull { packageManager.getLaunchIntentForPackage(it.activityInfo.packageName) }
        val preferred = launches.firstOrNull { it.component?.packageName == preferredPackage }
        val intent = when {
            preferred != null -> preferred
            launches.size == 1 -> launches.single()
            launches.size > 1 -> Intent.createChooser(launches.first(), "Open clock").apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, launches.drop(1).toTypedArray())
            }
            else -> alarms
        }
        open(intent, "No accessible clock app is installed.")
    }

    private fun chooseHome() {
        val roles = getSystemService(RoleManager::class.java)
        if (roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
            open(roles.createRequestRoleIntent(RoleManager.ROLE_HOME), "Unable to choose default home.")
        } else {
            open(Intent(Settings.ACTION_HOME_SETTINGS), "Home settings are unavailable.")
        }
    }

    private fun open(intent: Intent, failure: String) {
        runCatching { startActivity(intent) }.onFailure {
            Toast.makeText(this, failure, Toast.LENGTH_SHORT).show()
        }
    }
}
