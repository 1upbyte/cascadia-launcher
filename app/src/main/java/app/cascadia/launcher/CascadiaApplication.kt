package app.cascadia.launcher

import android.app.Application
import app.cascadia.launcher.data.AppRepository
import app.cascadia.launcher.data.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class CascadiaApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val apps by lazy { AppRepository(this, applicationScope) }
    val preferences by lazy { PreferencesRepository(this) }
}
