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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cascadia.launcher.R
import app.cascadia.launcher.LauncherViewModel
import app.cascadia.launcher.core.AppEntry
import app.cascadia.launcher.core.visiblePins
import app.cascadia.launcher.core.homeTopSpacing
import app.cascadia.launcher.core.sectionRow
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
    var railBottomMargin by remember(preferences.railBottomMargin) { mutableFloatStateOf(preferences.railBottomMargin) }
    var railSpacing by remember(preferences.railSpacing) { mutableFloatStateOf(preferences.railSpacing) }
    val appLayout = remember(appScale) { AppLayout(appScale) }
    val saveError by model.error.collectAsStateWithLifecycle()
    val pins = remember(preferences.pins, inventory.apps) { visiblePins(preferences.pins, inventory.apps) }
    val list = rememberLazyListState()
    var browsing by rememberSaveable { mutableStateOf(false) }
    var homeIndex by rememberSaveable { mutableIntStateOf(0) }
    var homeOffset by rememberSaveable { mutableIntStateOf(0) }
    var selectedLetter by remember { mutableStateOf<String?>(null) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var reorder by rememberSaveable { mutableStateOf(false) }
    var appOptions by remember { mutableStateOf<AppEntry?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
    val sections = index.sections
    val saveOrder by rememberUpdatedState(model::reorderPins)
    val arrangement = remember(list, view) {
        FavoriteArrangement(list, save = { saveOrder(it) },
            pickup = { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) })
    }
    LaunchedEffect(pins, reorder) { arrangement.sync(pins, reorder) }
    val density = LocalDensity.current
    LaunchedEffect(arrangement.draggedId) {
        if (arrangement.draggedId != null) {
            val edge = with(density) { 72.dp.toPx() }
            val speed = with(density) { 720.dp.toPx() }
            var previous = withFrameNanos { it }
            while (arrangement.draggedId != null) {
                val now = withFrameNanos { it }
                arrangement.scrollAtEdge(edge, speed, ((now - previous) / 1_000_000_000f).coerceAtMost(0.05f))
                previous = now
            }
        }
    }

    fun finishArrangement() {
        arrangement.finish()
        reorder = false
    }

    fun message(text: String) { Toast.makeText(context, text, Toast.LENGTH_SHORT).show() }
    fun openApps(letter: String, feedback: Boolean, scrollOffset: Int = 0) {
        finishArrangement()
        if (!browsing) {
            homeIndex = list.firstVisibleItemIndex
            homeOffset = list.firstVisibleItemScrollOffset
            browsing = true
        }
        if (selectedLetter != letter || !feedback) {
            // Position the selected section in the same remeasure as its reveal.
            // Releasing the rail never changes the scroll request.
            index.sectionRow(letter, pins.size, holding = false,
                includeHomePrefix = false)?.let {
                list.requestScrollToItem(it, scrollOffset)
            }
            if (feedback) {
                // One light platform tick per new letter, respecting system haptic
                // settings. No VIBRATE permission or repeating waveform required.
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }
        selectedLetter = if (feedback) letter else null
    }

    fun returnHome() {
        finishArrangement()
        browsing = false
        selectedLetter = null
        scope.launch { list.scrollToItem(0) }
    }

    LaunchedEffect(homeRequest) {
        if (homeRequest > 0) {
            browsing = false
            selectedLetter = null
            settings = false
            finishArrangement()
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
            reorder -> finishArrangement()
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
            val homeTop = homeTopSpacing(viewportHeight.value).dp
            var homeHeaderPixels by remember(viewportHeight, density, isDefaultHome) { mutableIntStateOf(0) }
            var headingPixels by remember(appLayout, density) { mutableIntStateOf(0) }
            val firstAppTop = if (homeHeaderPixels > 0) with(density) { homeHeaderPixels.toDp() }
                else homeTop + clockHeight + LauncherLayout.ClockFavoritesGap
            val headingHeight = if (headingPixels > 0) with(density) { headingPixels.toDp() }
                else with(density) { appLayout.labelLineHeight.toDp() } + 24.dp
            // Both views start their first app row at the same height. Browsing
            // includes a letter heading where home has its clock and date.
            val browseTopSpace = (firstAppTop - headingHeight).coerceAtLeast(0.dp)
            val footerHeight = rowHeight * 2 + 16.dp
            // Allow even the final category to start at the shared app height,
            // rather than moving it down to fill the bottom of the viewport.
            val browseBottomSpace = (viewportHeight - firstAppTop - rowHeight - footerHeight + 24.dp)
                .coerceAtLeast(24.dp)
            // Keep every item's key, size and scroll bounds unchanged during a
            // rail gesture. Release only reveals the surrounding sections.
            LazyColumn(
                state = list,
                modifier = Modifier.fillMaxSize().padding(end = LauncherLayout.RailTouchWidth).testTag("app-list")
                    .semantics { stateDescription = if (browsing) "All apps" else "Favorites" },
                contentPadding = PaddingValues(start = LauncherLayout.ContentStart,
                    bottom = if (browsing) browseBottomSpace else 24.dp),
            ) {
                if (!browsing) {
                    item(key = "home-header", contentType = "header") {
                        Column(Modifier.onSizeChanged { homeHeaderPixels = it.height }) {
                            Spacer(Modifier.height(homeTop))
                            HomeClock(onClick = onClock)
                            if (!isDefaultHome) {
                                TextButton(onClick = onChooseHome) { Text("Set as default home") }
                            }
                            Spacer(Modifier.height(LauncherLayout.ClockFavoritesGap))
                        }
                    }
                    items(if (reorder) arrangement.ordered else pins,
                        key = { "pin:${it.id}" }, contentType = { "app" }) { app ->
                        val dragging = arrangement.draggedId == app.id
                        Row(
                            Modifier.testTag("favorite:${app.id}")
                                .animateItem(placementSpec = if (dragging) null else androidx.compose.animation.core.spring())
                                .zIndex(if (dragging) 1f else 0f)
                                .graphicsLayer {
                                    translationY = arrangement.translation(app.id)
                                    scaleX = if (dragging) 1.03f else 1f
                                    scaleY = if (dragging) 1.03f else 1f
                                }
                                .shadow(if (dragging) 8.dp else 0.dp, MaterialTheme.shapes.medium)
                                .background(if (dragging) MaterialTheme.colorScheme.surface else Color.Transparent,
                                    MaterialTheme.shapes.medium),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppRow(app, model.apps, inventory.revision, preferences.iconShape, appLayout,
                                onClick = { if (!model.apps.launch(app.id)) message("This app is no longer available.") },
                                onLongClick = { appOptions = app },
                                modifier = Modifier.weight(1f), enabled = !reorder)
                            if (reorder) FavoriteDragHandle(app, arrangement)
                        }
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
                }
                if (browsing) {
                    sections.forEach { section ->
                        item(key = "section:${section.letter}", contentType = "section") {
                            RailContent(visible = selectedLetter == null || selectedLetter == section.letter) {
                                Column {
                                    if (section.letter == sections.firstOrNull()?.letter) {
                                        Spacer(Modifier.height(browseTopSpace).testTag("browse-top-space"))
                                    }
                                    Text(section.letter, color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = appLayout.labelSize, lineHeight = appLayout.labelLineHeight,
                                        modifier = Modifier.testTag("section-heading:${section.letter}")
                                            .onSizeChanged { headingPixels = it.height }
                                            .padding(top = 16.dp, bottom = 8.dp).semantics { heading() })
                                }
                            }
                        }
                        items(section.apps, key = { "app:${it.id}" }, contentType = { "app" }) { app ->
                            RailContent(visible = selectedLetter == null || selectedLetter == section.letter) {
                                AppRow(app, model.apps, inventory.revision, preferences.iconShape, appLayout,
                                    onClick = { if (selectedLetter == null && !model.apps.launch(app.id)) message("This app is no longer available.") },
                                    onLongClick = { if (selectedLetter == null) appOptions = app })
                            }
                        }
                    }
                    if (sections.isEmpty() && !inventory.loading) {
                        item(key = "empty") { Text("No launchable apps are available.") }
                    }
                    item(key = "launcher-footer", contentType = "footer") {
                        RailContent(visible = selectedLetter == null) {
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
                    item(key = "browse-tail", contentType = "spacer") {
                        Spacer(Modifier.fillMaxWidth().testTag("browse-tail"))
                    }
                }
            }
            AlphabetRail(
                occupied = remember(index) { sections.map { it.letter }.toSet() },
                selected = selectedLetter,
                onSelect = { letter ->
                    val offset = if (letter == sections.firstOrNull()?.letter) 0
                        else -with(density) { browseTopSpace.roundToPx() }
                    openApps(letter, feedback = true, scrollOffset = offset)
                },
                onReturnHome = ::returnHome,
                onRelease = { selectedLetter = null },
                onAccessibleOpen = { openApps(sections.firstOrNull()?.letter ?: "A", feedback = false) },
                spacing = railSpacing,
                modifier = Modifier.align(Alignment.BottomEnd).padding(top = 16.dp,
                    bottom = railBottomMargin.dp.coerceAtMost((viewportHeight - 64.dp).coerceAtLeast(0.dp))),
            )
            if (reorder) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 20.dp, vertical = 12.dp)
                        .testTag("arrange-favorites-controls"),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 3.dp,
                ) {
                    Row(Modifier.padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Arrange favorites", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = ::finishArrangement) { Text("Done") }
                    }
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
                    if (app.id in preferences.pins) {
                        TextButton(onClick = {
                            appOptions = null
                            browsing = false
                            selectedLetter = null
                            arrangement.sync(pins, true)
                            reorder = true
                            list.requestScrollToItem(0)
                        }) { Text("Arrange favorites") }
                    }
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
            railBottomMargin = railBottomMargin,
            onRailBottomMargin = { railBottomMargin = it },
            onRailBottomMarginFinished = model::setRailBottomMargin,
            railSpacing = railSpacing,
            onRailSpacing = { railSpacing = it },
            onRailSpacingFinished = model::setRailSpacing,
            iconShape = preferences.iconShape,
            onIconShape = model::setIconShape,
            isDefaultHome = isDefaultHome,
            onChooseHome = onChooseHome,
            onWallpaper = onWallpaper,
            onScrim = model::setScrim,
            onRefresh = { model.apps.refresh(); message("Refreshing apps") },
            onDismiss = { settings = false },
        )
    }
}

/** Hide surrounding rows without changing their measurement or lazy-list identity. */
@Composable
private fun RailContent(visible: Boolean, content: @Composable () -> Unit) {
    Box(if (visible) Modifier else Modifier.alpha(0f).clearAndSetSemantics {}) {
        content()
    }
}
