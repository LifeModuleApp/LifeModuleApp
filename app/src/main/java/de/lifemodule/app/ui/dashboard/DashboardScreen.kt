/*
 * LifeModule — A modular, privacy-focused life tracking app for Android.
 * Copyright (C) 2026 Paul Bernhard Colin Witzke
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.lifemodule.app.ui.dashboard

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import de.lifemodule.app.R
import de.lifemodule.app.ui.components.LMCard
import de.lifemodule.app.ui.navigation.AppRoute
import de.lifemodule.app.ui.theme.Black
import de.lifemodule.app.ui.theme.Border
import de.lifemodule.app.ui.theme.LocalAccentColor
import de.lifemodule.app.ui.theme.LocalModuleColors
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    // ── State from ViewModel (survives recomposition / navigation) ──────────
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val columnCount by viewModel.columnCount.collectAsStateWithLifecycle()

    // Dynamic accent colours from theme
    val accent = LocalAccentColor.current
    val moduleColorOverrides = LocalModuleColors.current
    val haptic = LocalHapticFeedback.current

    // Drag state - declared here so LaunchedEffect can read draggedIndex
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    // Stable local drag list - NOT keyed on `tiles` to avoid teardown during drag.
    // LaunchedEffect syncs it only when no drag is in progress (prevents stale-tile flash).
    val localTiles = remember { mutableStateListOf(*tiles.toTypedArray()) }
    LaunchedEffect(tiles) {
        if (draggedIndex == null) {
            localTiles.clear()
            localTiles.addAll(tiles)
        }
    }

    // ── Refresh tile list on screen resume (back from ModuleManager, etc.) ──
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── Edit mode ────────────────────────────────────────────────────────────
    var editMode by remember { mutableStateOf(false) }

    // ── Guard: show spinner if no tiles are available yet ──────────────────
    if (localTiles.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = accent)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_leaf_logo),
                            contentDescription = stringResource(R.string.app_dashboard_logo),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = accent)) { append("Life") }
                                withStyle(SpanStyle(color = Color.White)) { append("Module") }
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    // Grid size toggle (2->3->4->2)
                    IconButton(onClick = { viewModel.cycleColumnCount() }) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.GridView,
                                contentDescription = stringResource(R.string.app_dashboard_rastergroesse),
                                tint = accent
                            )
                            Text(
                                text = "${columnCount}x",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                    }
                    // Edit mode toggle (pencil = enter edit, check = done)
                    IconButton(onClick = { editMode = !editMode }) {
                        Icon(
                            imageVector = if (editMode) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (editMode) stringResource(R.string.app_dashboard_fertig) else stringResource(R.string.app_dashboard_layout_bearbeiten),
                            tint = if (editMode) MaterialTheme.colorScheme.primary else accent
                        )
                    }
                    // Settings
                    IconButton(onClick = {
                        navController.navigate(AppRoute.Settings) {
                            launchSingleTop = true
                        }
                    }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.app_dashboard_einstellungen),
                            tint = accent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = Black
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // ── Module tiles with variable spans ──────────────────────────────────────
            itemsIndexed(
                items = localTiles,
                key = { _, tile -> tile.module.id },
                span = { _, tile ->
                    GridItemSpan(tile.size.colSpan.coerceAtMost(columnCount))
                }
            ) { index, tile ->
                val isDragged = draggedIndex == index
                // Resolve effective accent: module-specific override ?? global dynamic
                val tileAccent = moduleColorOverrides[tile.module.id] ?: accent
                val elevation by animateDpAsState(
                    targetValue = if (isDragged) 8.dp else 0.dp,
                    label = "elevation"
                )
                var showSizeMenu by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        // Animate non-dragged items sliding to their new grid positions.
                        // Disabled on the actively dragged tile to avoid double-animation
                        // (the drag offset already handles its movement).
                        .then(if (!isDragged) Modifier.animateItem() else Modifier)
                        .zIndex(if (isDragged) 1f else 0f)
                        .offset {
                            if (isDragged) IntOffset(
                                dragOffsetX.roundToInt(),
                                dragOffsetY.roundToInt()
                            ) else IntOffset.Zero
                        }
                        .shadow(elevation, RoundedCornerShape(16.dp))
                        // Key = module ID (stable), NOT index.
                        // Using index would restart the gesture handler every time items swap
                        // positions during a drag, killing the drag mid-flight.
                        .pointerInput(tile.module.id) {
                            val gap12dp = 12.dp.toPx()
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    // Dynamically resolve current index to avoid stale closure capture
                                    val currentIdx = localTiles.indexOfFirst { it.module.id == tile.module.id }
                                    if (currentIdx >= 0) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        draggedIndex = currentIdx
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                    }
                                },
                                onDrag = { change, offset ->
                                    val drIdx = draggedIndex
                                    if (drIdx != null) {
                                        change.consume()
                                        dragOffsetX += offset.x
                                        dragOffsetY += offset.y

                                        // Normalize offset to 1-column-wide steps
                                        val cellW = (size.width.toFloat() / localTiles[drIdx].size.colSpan
                                            .coerceAtMost(columnCount).coerceAtLeast(1))
                                        val cellH = size.height.toFloat()

                                        val colShift = (dragOffsetX / (cellW + gap12dp)).roundToInt()
                                        val rowShift = (dragOffsetY / (cellH + gap12dp)).roundToInt()
                                        val target = (drIdx + rowShift * columnCount + colShift)
                                            .coerceIn(0, localTiles.lastIndex)

                                        if (target != drIdx && target in localTiles.indices) {
                                            val item = localTiles.removeAt(drIdx)
                                            localTiles.add(target, item)
                                            draggedIndex = target
                                            dragOffsetX = 0f
                                            dragOffsetY = 0f
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if (draggedIndex != null) {
                                        viewModel.applyDragOrder(localTiles.toList())
                                        draggedIndex = null
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                    }
                                },
                                onDragCancel = {
                                    draggedIndex = null
                                    dragOffsetX = 0f
                                    dragOffsetY = 0f
                                }
                            )
                        }
                ) {
                    LMCard(
                        modifier = Modifier
                            .aspectRatio(tile.size.aspectRatio)
                            .clickable {
                                if (draggedIndex == null) {
                                    if (editMode) {
                                        showSizeMenu = true
                                    } else {
                                        navController.navigate(tile.module.route) {
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            }
                    ) {
                        // Box required for badge .align(TopEnd) overlay.
                        // Column is placed directly inside - no redundant fillMaxSize wrapper.
                        Box(Modifier.fillMaxSize()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                val iconSize = when {
                                    columnCount >= 4 && tile.size == ModuleSize.SMALL -> 24.dp
                                    tile.size == ModuleSize.LARGE -> 48.dp
                                    else -> 36.dp
                                }
                                Icon(
                                    imageVector = tile.module.icon,
                                    contentDescription = stringResource(tile.module.displayNameRes),
                                    tint = tileAccent,
                                    modifier = Modifier.size(iconSize)
                                )
                                Spacer(modifier = Modifier.height(if (columnCount >= 4 && tile.size == ModuleSize.SMALL) 4.dp else 8.dp))
                                Text(
                                    text = stringResource(tile.module.displayNameRes),
                                    style = if (columnCount >= 4 && tile.size == ModuleSize.SMALL)
                                        MaterialTheme.typography.labelSmall
                                    else MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // ── Edit-mode: size badge top-right ─────────────────
                            if (editMode) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .background(
                                            color = tileAccent.copy(alpha = 0.88f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = tile.size.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Black
                                    )
                                }
                            }

                            // ── Size-selection dropdown (edit mode tap) ─────────
                            DropdownMenu(
                                expanded = showSizeMenu,
                                onDismissRequest = { showSizeMenu = false }
                            ) {
                                Text(
                                    text = stringResource(tile.module.displayNameRes),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                                HorizontalDivider()
                                ModuleSize.entries.forEach { size ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(size.label)
                                                if (tile.size == size) {
                                                    Spacer(Modifier.width(8.dp))
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = tileAccent,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.setTileSize(tile.module, size)
                                            showSizeMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── "+" tile - opens Module Manager ────────────────────────────────
            item(span = { GridItemSpan(1) }) {
                LMCard(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable {
                            navController.navigate(AppRoute.ModuleManager) {
                                launchSingleTop = true
                            }
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.app_dashboard_module_verwalten),
                                tint = Border,
                                modifier = Modifier.size(if (columnCount >= 4) 24.dp else 36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.app_dashboard_module),
                                style = if (columnCount >= 4) MaterialTheme.typography.labelSmall
                                else MaterialTheme.typography.titleSmall,
                                color = Border
                            )
                        }
                    }
                }
            }
        }
    }
}
