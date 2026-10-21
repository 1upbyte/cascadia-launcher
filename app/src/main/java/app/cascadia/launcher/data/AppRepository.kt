package app.cascadia.launcher.data

import android.content.ComponentName
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import app.cascadia.launcher.core.AppEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppInventory(
    val apps: List<AppEntry> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val revision: Int = 0,
)

class AppRepository(context: Context, scope: CoroutineScope) {
    private val launcher = context.getSystemService(LauncherApps::class.java)
    private val ownPackage = context.packageName
    private val user = Process.myUserHandle()
    private val mutableInventory = MutableStateFlow(AppInventory())
    val inventory = mutableInventory.asStateFlow()
    private val refreshes = Channel<Unit>(Channel.CONFLATED)
    private val icons = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = refresh()
        override fun onPackageAdded(packageName: String, user: UserHandle) = refresh()
        override fun onPackageChanged(packageName: String, user: UserHandle) = refresh()
        override fun onPackagesAvailable(names: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(names: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
    }

    init {
        launcher.registerCallback(callback)
        ContextCompat.registerReceiver(context, object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = refresh()
        }, IntentFilter(Intent.ACTION_LOCALE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        scope.launch(Dispatchers.IO) {
            for (ignored in refreshes) {
                runCatching {
                    launcher.getActivityList(null, user)
                        .filter { it.componentName.packageName != ownPackage }
                        .map { AppEntry(it.componentName.flattenToString(), it.label.toString()) }
                }.onSuccess { apps ->
                    icons.evictAll()
                    mutableInventory.value = AppInventory(
                        apps = apps, loading = false, revision = mutableInventory.value.revision + 1,
                    )
                }.onFailure {
                    mutableInventory.value = mutableInventory.value.copy(
                        loading = false, error = "Unable to load apps. Open settings to retry.",
                    )
                }
            }
        }
        refresh()
    }

    fun refresh() { refreshes.trySend(Unit) }

    fun cachedIcon(id: String, size: Int, revision: Int): Bitmap? =
        icons.get("$id:$size:$revision")

    suspend fun icon(id: String, size: Int, revision: Int): Bitmap? {
        cachedIcon(id, size, revision)?.let { return it }
        return withContext(Dispatchers.IO) {
            val key = "$id:$size:$revision"
            icons.get(key) ?: runCatching {
                val component = ComponentName.unflattenFromString(id) ?: return@withContext null
                launcher.getActivityList(component.packageName, user)
                    .firstOrNull { it.componentName == component }
                    ?.getBadgedIcon(0)?.let { renderIcon(it, size) }?.also { icons.put(key, it) }
            }.getOrNull()
        }
    }

    fun launch(id: String): Boolean = runCatching {
        val component = requireNotNull(ComponentName.unflattenFromString(id))
        launcher.startMainActivity(component, user, null, null)
    }.isSuccess

    fun appInfo(id: String): Boolean = runCatching {
        val component = requireNotNull(ComponentName.unflattenFromString(id))
        launcher.startAppDetailsActivity(component, user, null, null)
    }.isSuccess
}

/** Draw adaptive layers directly so the system's circular mask is not baked in. */
internal fun renderIcon(drawable: Drawable, size: Int): Bitmap {
    if (drawable !is AdaptiveIconDrawable) return drawable.toBitmap(size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    // Setting adaptive bounds also supplies the platform's overscan bounds to its layers.
    drawable.setBounds(0, 0, size, size)
    drawable.background?.draw(canvas)
    drawable.foreground?.draw(canvas)
    return bitmap
}
