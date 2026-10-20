package app.cascadia.launcher.ui

import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cascadia.launcher.LauncherViewModel
import app.cascadia.launcher.core.AppEntry
import app.cascadia.launcher.core.visiblePins
import kotlinx.coroutines.launch

@Composable
fun LauncherScreen(
    homeRequest: Int,
    isDefaultHome: Boolean,
    onChooseHome: () -> Unit,
    onWallpaper: () -> Unit,
    onClock: () -> Unit,
    model: LauncherViewModel = viewModel(),
) {
    val inventory by model.inventory.collectAsStateWithLifecycle()
    val index by model.index.collectAsStateWithLifecycle()
    val preferences by model.preferences.collectAsStateWithLifecycle()
    val saveError by model.error.collectAsStateWithLifecycle()
    val pins = remember(preferences.pins, inventory.apps) { visiblePins(preferences.pins, inventory.apps) }
    val list = rememberLazyListState()
    var browsing by rememberSaveable { mutableStateOf(false) }
    var homeIndex by rememberSaveable { mutableIntStateOf(0) }
    var homeOffset by rememberSaveable { mutableIntStateOf(0) }
    var requestedLetter by rememberSaveable { mutableStateOf<String?>(null) }
    var navigationRequest by remember { mutableIntStateOf(0) }
    var handledNavigation by remember { mutableIntStateOf(0) }
    var selectedLetter by remember { mutableStateOf<String?>(null) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var reorder by rememberSaveable { mutableStateOf(false) }
    var appOptions by remember { mutableStateOf<AppEntry?>(null) }
    var lastHaptic by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    fun message(text: String) { Toast.makeText(context, text, Toast.LENGTH_SHORT).show() }
    fun openApps(letter: String, feedback: Boolean) {
        if (!browsing) {
            homeIndex = list.firstVisibleItemIndex
            homeOffset = list.firstVisibleItemScrollOffset
            browsing = true
        }
        if (selectedLetter != letter || !feedback) {
            requestedLetter = letter
            navigationRequest++
            if (feedback && SystemClock.uptimeMillis() - lastHaptic > 40) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                lastHaptic = SystemClock.uptimeMillis()
            }
        }
        selectedLetter = if (feedback) letter else null
    }

    LaunchedEffect(navigationRequest, index, pins.size) {
        val request = navigationRequest
        // Consume each explicit rail request only once. Inventory changes and
        // activity recreation must not jump a freely scrolled list back to a letter.
        if (request > handledNavigation && browsing && requestedLetter != null) {
            val target = index.nearestSection(requestedLetter!!)
            if (target != null) {
                var row = pins.size + 2 // home header + pins + home help
                for (section in index.sections) {
                    if (section.letter == target.letter) break
                    row += 1 + section.apps.size
                }
                list.scrollToItem(row)
                handledNavigation = request
            }
        }
    }

    LaunchedEffect(homeRequest) {
        if (homeRequest > 0) {
            browsing = false
            selectedLetter = null
            requestedLetter = null
            settings = false
            reorder = false
            appOptions = null
            list.scrollToItem(0)
        }
    }
    LaunchedEffect(saveError) {
        saveError?.let { message(it); model.clearError() }
    }
    // Always consume root Back, including the frame in which a dialog closes.
    // Conditional callbacks can briefly leave a gap and finish the Home activity.
    BackHandler {
        when {
            appOptions != null -> appOptions = null
            settings -> settings = false
            reorder -> reorder = false
            browsing -> {
                browsing = false
                requestedLetter = null
                selectedLetter = null
                scope.launch { list.scrollToItem(homeIndex.coerceAtMost(pins.size + 1), homeOffset) }
            }
            else -> scope.launch { list.animateScrollToItem(0) }
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = preferences.scrim))
            .windowInsetsPadding(WindowInsets.safeDrawing.union(
                WindowInsets.systemGestures.only(WindowInsetsSides.End),
            )),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val viewportHeight = maxHeight
            val lastSectionRows = index.sections.lastOrNull()?.apps?.size ?: 0
            val tailHeight = (viewportHeight - 112.dp - 64.dp * lastSectionRows).coerceAtLeast(0.dp)
            LazyColumn(
                state = list,
                modifier = Modifier.fillMaxSize().padding(end = 60.dp).testTag("app-list")
                    .semantics { stateDescription = if (browsing) "All apps" else "Favorites" },
                contentPadding = PaddingValues(start = 24.dp, end = 8.dp, top = 24.dp, bottom = 24.dp),
            ) {
                item(key = "home-header", contentType = "header") {
                    Column {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("cascadia", fontSize = 16.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { settings = true }) { Text("Settings") }
                        }
                        HomeClock(onClick = onClock)
                        if (!isDefaultHome) {
                            TextButton(onClick = onChooseHome) { Text("Set as default home") }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("Favorites", style = MaterialTheme.typography.labelLarge, modifier = Modifier.semantics { heading() })
                        Spacer(Modifier.height(8.dp))
                    }
                }
                items(pins, key = { "pin:${it.id}" }, contentType = { "app" }) { app ->
                    AppRow(app, model.apps, inventory.revision,
                        onClick = { if (!model.apps.launch(app.id)) message("This app is no longer available.") },
                        onLongClick = { appOptions = app })
                }
                item(key = "home-help", contentType = "help") {
                    Column(Modifier.padding(vertical = 24.dp)) {
                        when {
                            inventory.loading -> Text("Loading apps…")
                            inventory.error != null -> Text(inventory.error!!)
                            pins.isEmpty() -> Text("Make room for what matters.\nTouch the alphabet, then long-press an app to pin it.",
                                style = MaterialTheme.typography.bodyLarge)
                        }
                        if (pins.isNotEmpty()) {
                            Text("Slide along the alphabet to browse.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (browsing) {
                    index.sections.forEach { section ->
                        item(key = "section:${section.letter}", contentType = "section") {
                            Text(section.letter, color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 12.dp).semantics { heading() })
                        }
                        items(section.apps, key = { "app:${it.id}" }, contentType = { "app" }) { app ->
                            AppRow(app, model.apps, inventory.revision,
                                onClick = { if (!model.apps.launch(app.id)) message("This app is no longer available.") },
                                onLongClick = { appOptions = app })
                        }
                    }
                    if (index.sections.isEmpty() && !inventory.loading) {
                        item(key = "empty") { Text("No launchable apps found. Retry from Settings.") }
                    }
                    // Permit even the final letter to align at the top, hiding clock/pins.
                    item(key = "browse-tail", contentType = "spacer") {
                        Spacer(Modifier.fillMaxWidth().height(tailHeight).testTag("browse-tail"))
                    }
                }
            }
        }
        AlphabetRail(
            occupied = remember(index) { index.sections.map { it.letter }.toSet() },
            selected = selectedLetter,
            onSelect = { openApps(it, feedback = true) },
            onRelease = { selectedLetter = null },
            onAccessibleOpen = { openApps(index.sections.firstOrNull()?.letter ?: "A", feedback = false) },
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp, top = 16.dp, bottom = 16.dp),
        )
        selectedLetter?.let { letter ->
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 76.dp).size(88.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(letter, fontSize = 44.sp, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }

    appOptions?.let { app ->
        AlertDialog(
            onDismissRequest = { appOptions = null },
            title = { Text(app.label) },
            text = {
                Column {
                    TextButton(onClick = { model.togglePin(app.id); appOptions = null }) {
                        Text(if (app.id in preferences.pins) "Unpin from favorites" else "Pin to favorites")
                    }
                    TextButton(onClick = {
                        appOptions = null
                        if (!model.apps.appInfo(app.id)) message("App info is unavailable.")
                    }) { Text("App info") }
                }
            },
            confirmButton = { TextButton(onClick = { appOptions = null }) { Text("Done") } },
        )
    }
    if (settings) {
        SettingsDialog(
            scrim = preferences.scrim,
            isDefaultHome = isDefaultHome,
            onChooseHome = onChooseHome,
            onWallpaper = onWallpaper,
            onScrim = model::setScrim,
            onReorder = { settings = false; reorder = true },
            onRefresh = { model.apps.refresh(); message("Refreshing apps") },
            onDismiss = { settings = false },
        )
    }
    if (reorder) {
        ReorderDialog(pins, onMove = model::movePin, onUnpin = model::togglePin, onDismiss = { reorder = false })
    }
}
