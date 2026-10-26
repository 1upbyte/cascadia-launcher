package app.cascadia.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cascadia.launcher.core.AppIndex
import app.cascadia.launcher.data.LauncherPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CascadiaApplication
    val apps = app.apps
    val inventory = apps.inventory
    val index = inventory.map { AppIndex.build(it.apps) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppIndex(emptyList()))
    val preferences = app.preferences.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, LauncherPreferences())

    // Preserve edit ordering even during fast reorder gestures; DataStore serializes writes.
    private val edits = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    init {
        viewModelScope.launch {
            for (edit in edits) {
                runCatching { edit() }.onFailure { mutableError.value = "Could not save settings. Please retry." }
            }
        }
    }
    private val mutableError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val error = mutableError
    fun clearError() { mutableError.value = null }
    fun togglePin(id: String) { edits.trySend { app.preferences.togglePin(id) } }
    fun movePin(id: String, delta: Int) { edits.trySend { app.preferences.movePin(id, delta) } }
    fun setIconShape(value: app.cascadia.launcher.core.IconShape) { edits.trySend { app.preferences.setIconShape(value) } }
    fun setAppScale(value: Float) { edits.trySend { app.preferences.setAppScale(value) } }
    fun setRailBottomMargin(value: Float) { edits.trySend { app.preferences.setRailBottomMargin(value) } }
    fun setRailSpacing(value: Float) { edits.trySend { app.preferences.setRailSpacing(value) } }
    fun setScrim(value: Float) { edits.trySend { app.preferences.setScrim(value) } }

}
