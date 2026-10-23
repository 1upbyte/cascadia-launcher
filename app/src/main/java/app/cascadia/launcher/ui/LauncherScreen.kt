package app.cascadia.launcher.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cascadia.launcher.R
import app.cascadia.launcher.LauncherViewModel
import app.cascadia.launcher.core.AppEntry
import app.cascadia.launcher.core.visiblePins
import app.cascadia.launcher.core.homeTopSpacing
import app.cascadia.launcher.core.browseSections
import app.cascadia.launcher.core.sectionRow
import app.cascadia.launcher.core.browseSectionTop
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
    var appScale by remember(preferences.appScale) { mutableFloatStateOf(preferences.appScale) }
    val appLayout = remember(appScale) { AppLayout(appScale) }
    val saveError by model.error.collectAsStateWithLifecycle()
    val pins = remember(preferences.pins, inventory.apps) { visiblePins(preferences.pins, inventory.apps) }
    val list = rememberLazyListState()
    var browsing by rememberSaveable { mutableStateOf(false) }
    var homeIndex by rememberSaveable { mutableIntStateOf(0) }
    var homeOffset by rememberSaveable { mutableIntStateOf(0) }
    var selectedLetter by remember { mutableStateOf<String?>(null) }
    var anchoredLetter by rememberSaveable { mutableStateOf<String?>(null) }
    var heldSectionTopPx by remember { mutableIntStateOf(0) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var reorder by rememberSaveable { mutableStateOf(false) }
    var appOptions by remember { mutableStateOf<AppEntry?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
    val sections = remember(index, selectedLetter) { index.browseSections(selectedLetter) }

    fun message(text: String) { Toast.makeText(context, text, Toast.LENGTH_SHORT).show() }
    fun openApps(letter: String, feedback: Boolean) {
        if (!browsing) {
            homeIndex = list.firstVisibleItemIndex
            homeOffset = list.firstVisibleItemScrollOffset
            browsing = true
        }
        if (selectedLetter != letter || !feedback) {
            // Apply the jump in the same remeasure as the section change,
            // not a coroutine/recomposition later. Only runs on letter changes.
            index.sectionRow(letter, pins.size, holding = feedback)?.let {
                list.requestScrollToItem(it)
            }
            if (feedback) {
                // One light platform tick per new letter, respecting system haptic
                // settings. No VIBRATE permission or repeating waveform required.
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }
        anchoredLetter = if (feedback) letter else null
        selectedLetter = if (feedback) letter else null
    }

    fun releaseLetter() {
        val letter = selectedLetter ?: return
        selectedLetter = null
        // Remove the temporary positioning spacer from the restored full list,
        // then offset the section item to keep its heading and apps in place.
        anchoredLetter = null
        if (browsing) index.sectionRow(letter, pins.size, holding = false)?.let {
            list.requestScrollToItem(it, -heldSectionTopPx)
        }
    }

    LaunchedEffect(homeRequest) {
        if (homeRequest > 0) {
            browsing = false
            selectedLetter = null
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
                selectedLetter = null
                scope.launch { list.scrollToItem(homeIndex.coerceAtMost(pins.size + 1), homeOffset) }
            }
            else -> scope.launch { list.animateScrollToItem(0) }
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = preferences.scrim))
            .safeDrawingPadding(),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val viewportHeight = maxHeight
            val density = LocalDensity.current
            val rowHeight = maxOf(appLayout.rowMinHeight,
                with(density) { appLayout.labelLineHeight.toDp() } + appLayout.rowPadding * 2)
            val clockHeight = with(density) {
                LauncherLayout.ClockLineHeight.toDp() + LauncherLayout.DateLineHeight.toDp()
            }
            val homeGroupHeight = clockHeight + LauncherLayout.ClockFavoritesGap + rowHeight * pins.size + 48.dp
            val homeTop = homeTopSpacing(viewportHeight.value, homeGroupHeight.value).dp
            val headingHeight = with(density) { appLayout.labelLineHeight.toDp() } + 24.dp
            val anchorSlot = index.sections.indexOfFirst { it.letter == anchoredLetter }
            val remainingSections = if (anchorSlot >= 0) index.sections.drop(anchorSlot) else emptyList()
            val footerHeight = rowHeight * 2 + 16.dp
            val remainingHeight = remainingSections.fold(footerHeight) { height, section ->
                height + headingHeight + rowHeight * section.apps.size
            }
            val heldTop = if (anchorSlot >= 0) browseSectionTop(
                viewportHeight.value, index.sections.size,
                with(density) { 19.sp.toDp().value }, remainingHeight.value, bottomInset = 192.dp.value,
            ).dp else 0.dp
            SideEffect { heldSectionTopPx = with(density) { heldTop.roundToPx() } }
            // While holding, reserve exactly the space occupied by later sections.
            // Release swaps this blank area for real rows without changing scroll bounds.
            val tailHeight = if (selectedLetter != null) {
                (remainingHeight - headingHeight - rowHeight * (remainingSections.firstOrNull()?.apps?.size ?: 0))
                    .coerceAtLeast(0.dp)
            } else 0.dp
            LazyColumn(
                state = list,
                modifier = Modifier.fillMaxSize().padding(end = LauncherLayout.RailTouchWidth).testTag("app-list")
                    .semantics { stateDescription = if (browsing) "All apps" else "Favorites" },
                contentPadding = PaddingValues(start = LauncherLayout.ContentStart, bottom = 24.dp),
            ) {
                item(key = "home-header", contentType = "header") {
                    Column {
                        Spacer(Modifier.height(homeTop))
                        HomeClock(onClick = onClock)
                        if (!isDefaultHome) {
                            TextButton(onClick = onChooseHome) { Text("Set as default home") }
                        }
                        Spacer(Modifier.height(LauncherLayout.ClockFavoritesGap))
                    }
                }
                items(pins, key = { "pin:${it.id}" }, contentType = { "app" }) { app ->
                    AppRow(app, model.apps, inventory.revision, preferences.iconShape, appLayout,
                        onClick = { if (!model.apps.launch(app.id)) message("This app is no longer available.") },
                        onLongClick = { appOptions = app })
                }
                item(key = "home-help", contentType = "help") {
                    Column(Modifier.padding(top = if (pins.isEmpty() || inventory.loading || inventory.error != null) 16.dp else 0.dp,
                        bottom = if (pins.isEmpty() || inventory.loading || inventory.error != null) 24.dp else 0.dp)) {
                        when {
                            inventory.loading -> Text("Loading apps…")
                            inventory.error != null -> Text(inventory.error!!)
                            pins.isEmpty() -> Text("Make room for what matters.\nTouch the alphabet, then long-press an app to pin it.",
                                style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                if (browsing) {
                    sections.forEach { section ->
                        item(key = "section:${section.letter}", contentType = "section") {
                            Column {
                                // Space belongs to this section's item, not a negative
                                // scroll offset that could reveal clock/favorites above it.
                                if (section.letter == anchoredLetter) Spacer(Modifier.height(heldTop))
                                Text(section.letter, color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = appLayout.labelSize, lineHeight = appLayout.labelLineHeight,
                                    modifier = Modifier.testTag("section-heading:${section.letter}")
                                        .padding(top = 16.dp, bottom = 8.dp).semantics { heading() })
                            }
                        }
                        items(section.apps, key = { "app:${it.id}" }, contentType = { "app" }) { app ->
                            AppRow(app, model.apps, inventory.revision, preferences.iconShape, appLayout,
                                onClick = { if (!model.apps.launch(app.id)) message("This app is no longer available.") },
                                onLongClick = { appOptions = app })
                        }
                    }
                    if (index.sections.isEmpty() && !inventory.loading) {
                        item(key = "empty") { Text("No launchable apps found. Retry from Settings.") }
                    }
                    if (selectedLetter == null) {
                        item(key = "launcher-footer", contentType = "footer") {
                            Column(Modifier.padding(top = 16.dp).testTag("launcher-footer")) {
                                FooterAppRow("Cascadia Settings", R.drawable.ic_settings, preferences.iconShape, appLayout) { settings = true }
                                FooterAppRow("GitHub", R.drawable.ic_github, preferences.iconShape, appLayout) {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW,
                                            Uri.parse("https://github.com/1upbyte/cascadia-launcher")))
                                    } catch (_: ActivityNotFoundException) {
                                        message("No app available to open GitHub.")
                                    }
                                }
                            }
                        }
                    }
                    // No artificial bottom runway after release.
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
            onRelease = ::releaseLetter,
            onAccessibleOpen = { openApps(index.sections.firstOrNull()?.letter ?: "A", feedback = false) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(top = 16.dp, bottom = 192.dp),
        )
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
            appScale = appScale,
            onAppScale = { appScale = it },
            onAppScaleFinished = model::setAppScale,
            iconShape = preferences.iconShape,
            onIconShape = model::setIconShape,
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
