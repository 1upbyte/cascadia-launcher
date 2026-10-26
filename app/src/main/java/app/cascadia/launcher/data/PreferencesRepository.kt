package app.cascadia.launcher.data

import android.content.Context
import app.cascadia.launcher.core.IconShape
import app.cascadia.launcher.core.AppScale
import app.cascadia.launcher.core.RailSettings
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
    val iconShape: IconShape = IconShape.SQUIRCLE, val appScale: Float = 1f,
    val railBottomMargin: Float = RailSettings.DEFAULT_BOTTOM_MARGIN,
    val railSpacing: Float = RailSettings.DEFAULT_SPACING)

class PreferencesRepository(private val context: Context) {
    private val pinsKey = stringPreferencesKey("pins")
    private val iconShapeKey = stringPreferencesKey("icon_shape")
    private val appScaleKey = floatPreferencesKey("app_scale")
    private val railBottomMarginKey = floatPreferencesKey("rail_bottom_margin")
    private val railSpacingKey = floatPreferencesKey("rail_spacing")
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
            IconShape.fromStored(stored[iconShapeKey]), AppScale.normalize(stored[appScaleKey] ?: 1f),
            RailSettings.normalizeBottomMargin(stored[railBottomMarginKey] ?: RailSettings.DEFAULT_BOTTOM_MARGIN),
            RailSettings.normalizeSpacing(stored[railSpacingKey] ?: RailSettings.DEFAULT_SPACING))
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

    suspend fun setRailBottomMargin(value: Float) = context.dataStore.edit {
        it[railBottomMarginKey] = RailSettings.normalizeBottomMargin(value)
    }

    suspend fun setRailSpacing(value: Float) = context.dataStore.edit {
        it[railSpacingKey] = RailSettings.normalizeSpacing(value)
    }

    suspend fun setScrim(value: Float) = context.dataStore.edit {
        it[scrimKey] = value.coerceIn(0f, 0.8f)
    }
}
