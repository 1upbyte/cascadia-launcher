package app.cascadia.launcher.data

import android.content.Context
import app.cascadia.launcher.core.IconShape
import app.cascadia.launcher.core.AppScale
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.dataStore by preferencesDataStore(
    name = "launcher",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

data class LauncherPreferences(val pins: List<String> = emptyList(), val scrim: Float = 0.35f,
    val iconShape: IconShape = IconShape.SQUIRCLE, val appScale: Float = 1f)

class PreferencesRepository(private val context: Context) {
    private val pinsKey = stringPreferencesKey("pins")
    private val iconShapeKey = stringPreferencesKey("icon_shape")
    private val appScaleKey = floatPreferencesKey("app_scale")
    private val scrimKey = floatPreferencesKey("scrim")

    val preferences = context.dataStore.data.catch { error ->
        if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
        else throw error
    }.map { stored ->
        val pins = runCatching {
            val array = JSONArray(stored[pinsKey] ?: "[]")
            List(array.length()) { array.getString(it) }.distinct()
        }.getOrDefault(emptyList())
        LauncherPreferences(pins, (stored[scrimKey] ?: 0.35f).coerceIn(0f, 0.8f),
            IconShape.fromStored(stored[iconShapeKey]), AppScale.normalize(stored[appScaleKey] ?: 1f))
    }

    suspend fun togglePin(id: String) = context.dataStore.edit { stored ->
        val array = runCatching { JSONArray(stored[pinsKey] ?: "[]") }.getOrElse { JSONArray() }
        val pins = List(array.length()) { array.getString(it) }.distinct()
        stored[pinsKey] = JSONArray(if (id in pins) pins - id else pins + id).toString()
    }

    suspend fun movePin(id: String, delta: Int) = context.dataStore.edit { stored ->
        val array = runCatching { JSONArray(stored[pinsKey] ?: "[]") }.getOrElse { JSONArray() }
        val pins = List(array.length()) { array.getString(it) }.distinct()
        stored[pinsKey] = JSONArray(app.cascadia.launcher.core.movePin(pins, id, delta)).toString()
    }

    suspend fun setIconShape(value: IconShape) = context.dataStore.edit {
        it[iconShapeKey] = value.name
    }

    suspend fun setAppScale(value: Float) = context.dataStore.edit {
        it[appScaleKey] = AppScale.normalize(value)
    }

    suspend fun setScrim(value: Float) = context.dataStore.edit {
        it[scrimKey] = value.coerceIn(0f, 0.8f)
    }
}
