package com.stormpanda.megingiard.macropad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stormpanda.megingiard.R
import com.stormpanda.megingiard.ui.AppDivider
import com.stormpanda.megingiard.ui.AppDropdown
import com.stormpanda.megingiard.ui.AppSelectableChip
import com.stormpanda.megingiard.ui.AppSettingsRow
import com.stormpanda.megingiard.ui.LocalAppColors
import java.util.Locale
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private const val TAG = "EditorLayoutComponents"

@Composable
internal fun EditorProfileChipsBar(
    profiles: List<PadProfile>,
    activeProfile: PadProfile?,
    onSelectProfile: (String) -> Unit,
    onEditProfile: (PadProfile) -> Unit,
    onDuplicateProfile: () -> Unit,
    onReorderProfiles: () -> Unit,
    onDeleteProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val latestProfiles by rememberUpdatedState(profiles)
    var menuExpanded by remember { mutableStateOf(false) }
    val canDelete = profiles.size > 1

    val listState = rememberLazyListState()
    val reorderState =
        rememberReorderableLazyListState(listState) { from, to ->
            val fromIdx = latestProfiles.indexOfFirst { it.id == from.key as? String }
            val toIdx = latestProfiles.indexOfFirst { it.id == to.key as? String }
            if (fromIdx >= 0 && toIdx >= 0) {
                val mutable = latestProfiles.toMutableList()
                mutable.add(toIdx, mutable.removeAt(fromIdx))
                MacroPadState.reorderProfiles(mutable)
            }
        }

    LaunchedEffect(activeProfile?.id, profiles) {
        val activeId = activeProfile?.id ?: return@LaunchedEffect
        val index = profiles.indexOfFirst { it.id == activeId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(profiles, key = { it.id }) { profile ->
                ReorderableItem(reorderState, key = profile.id) { isDragging ->
                    val isActive = profile.id == activeProfile?.id
                    AppSelectableChip(
                        text = profile.name,
                        selected = isActive,
                        isDragging = isDragging,
                        onClick = { onSelectProfile(profile.id) },
                        onDoubleClick = { onEditProfile(profile) },
                        modifier = Modifier.longPressDraggableHandle(),
                    )
                }
            }
        }

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.cd_more_options),
                    tint = colors.onSurfaceSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(colors.surface),
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.macropad_editor_title_edit_profile),
                            color = colors.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        activeProfile?.let { onEditProfile(it) }
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.macropad_duplicate_profile),
                            color = colors.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onDuplicateProfile()
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.macropad_reorder_profiles),
                            color = colors.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onReorderProfiles()
                    },
                )
                if (canDelete) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.macropad_editor_delete_profile),
                                color = colors.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDeleteProfile()
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun EditorLayoutChipsBar(
    layouts: List<PadLayout>,
    activeLayout: PadLayout?,
    profile: PadProfile,
    accentColor: Color,
    gridMode: GridMode,
    isCanvasLocked: Boolean,
    onToggleCanvasLock: () -> Unit,
    onAddButton: () -> Unit,
    onGridModeChange: (GridMode) -> Unit,
    onManageBackground: () -> Unit,
    onManageTouchpadSettings: () -> Unit,
    isBackgroundHidden: Boolean = false,
    onToggleBackgroundVisibility: (() -> Unit)? = null,
    onChangeGridCols: ((Int) -> Unit)? = null,
    onManageGridSize: (() -> Unit)? = null,
    onToggleGridBorders: (() -> Unit)? = null,
    onToggleGridButtonBg: (() -> Unit)? = null,
    onLayoutModeChange: (PadLayoutMode) -> Unit,
    onSelectLayout: (String) -> Unit,
    onEditLayout: (PadLayout) -> Unit,
    onDuplicateLayout: () -> Unit,
    onCopyToProfile: () -> Unit,
    onReorderLayouts: () -> Unit,
    onDeleteLayout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val latestLayouts by rememberUpdatedState(layouts)
    var menuExpanded by remember { mutableStateOf(false) }
    val canDelete = layouts.size > 1

    val lazyRowState = rememberLazyListState()
    val reorderState =
        rememberReorderableLazyListState(lazyRowState) { from, to ->
            val fromIdx = latestLayouts.indexOfFirst { it.id == from.key as? String }
            val toIdx = latestLayouts.indexOfFirst { it.id == to.key as? String }
            if (fromIdx >= 0 && toIdx >= 0) {
                val mutable = latestLayouts.toMutableList()
                mutable.add(toIdx, mutable.removeAt(fromIdx))
                MacroPadState.reorderLayouts(mutable)
            }
        }

    LaunchedEffect(activeLayout?.id, layouts) {
        val activeId = activeLayout?.id ?: return@LaunchedEffect
        val index = layouts.indexOfFirst { it.id == activeId }
        if (index >= 0) {
            lazyRowState.animateScrollToItem(index)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LazyRow(
                state = lazyRowState,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(layouts, key = { it.id }) { layout ->
                    ReorderableItem(reorderState, key = layout.id) { isDragging ->
                        val isActive = layout.id == activeLayout?.id
                        AppSelectableChip(
                            text = layout.name,
                            selected = isActive,
                            isDragging = isDragging,
                            onClick = { onSelectLayout(layout.id) },
                            onDoubleClick = { onEditLayout(layout) },
                            leadingIcon = { contentColor ->
                                Icon(
                                    imageVector = if (layout.isGridMode) Icons.Rounded.GridView else Icons.Rounded.OpenWith,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                            modifier = Modifier.longPressDraggableHandle(),
                        )
                    }
                }
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.cd_more_options),
                        tint = colors.onSurfaceSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(colors.surface),
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.macropad_editor_title),
                                color = colors.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            activeLayout?.let { onEditLayout(it) }
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.macropad_duplicate_layout),
                                color = colors.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDuplicateLayout()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.macropad_editor_copy_to_profile),
                                color = colors.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onCopyToProfile()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.macropad_reorder_layouts),
                                color = colors.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onReorderLayouts()
                        },
                    )
                    if (canDelete) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.macropad_editor_delete_layout),
                                    color = colors.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDeleteLayout()
                            },
                        )
                    }
                }
            }
        }

        // Layout mode chips (自由模式 ↔ 表格模式) + Wallpaper icon + Action Toolbar on the right
        val isGrid = activeLayout?.isGridMode == true
        val bgLabel = stringResource(R.string.macropad_editor_change_background)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.macropad_editor_mode_prefix),
                    color = colors.sectionHeaderColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                AppSelectableChip(
                    text = stringResource(R.string.layout_settings_mode_free),
                    selected = !isGrid,
                    onClick = { if (isGrid) onLayoutModeChange(PadLayoutMode.FREE) },
                    leadingIcon = { contentColor ->
                        Icon(
                            imageVector = Icons.Rounded.OpenWith,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(14.dp),
                        )
                    },
                )
                AppSelectableChip(
                    text = stringResource(R.string.layout_settings_mode_grid),
                    selected = isGrid,
                    onClick = { if (!isGrid) onLayoutModeChange(PadLayoutMode.GRID) },
                    leadingIcon = { contentColor ->
                        Icon(
                            imageVector = Icons.Rounded.GridView,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(14.dp),
                        )
                    },
                )
                EditorToolbarIconButton(
                    label = bgLabel,
                    icon = Icons.Rounded.Wallpaper,
                    accentColor = accentColor,
                    onClick = onManageBackground,
                    onLongClick = onToggleBackgroundVisibility,
                    isStrikethrough = isBackgroundHidden,
                )
            }

            // Action toolbar (Add Button / Lock / Touchpad / Grid OR Thick borders / Button bg / Grid size in Table mode)
            EditorToolbar(
                profile = profile,
                layout = activeLayout,
                accentColor = accentColor,
                gridMode = gridMode,
                isCanvasLocked = isCanvasLocked,
                onToggleCanvasLock = onToggleCanvasLock,
                onAddButton = onAddButton,
                onGridModeChange = {
                    val nextMode =
                        when (gridMode) {
                            GridMode.OFF -> GridMode.RECTANGULAR
                            GridMode.RECTANGULAR -> GridMode.RADIAL
                            GridMode.RADIAL -> GridMode.OFF
                        }
                    onGridModeChange(nextMode)
                },
                onManageTouchpadSettings = onManageTouchpadSettings,
                onChangeGridCols = onChangeGridCols,
                onManageGridSize = onManageGridSize,
                onToggleGridBorders = onToggleGridBorders,
                onToggleGridButtonBg = onToggleGridButtonBg,
            )
        }
    }
}

@Composable
internal fun ButtonColorStyleRow(
    label: String,
    selected: ButtonColorStyle,
    onSelect: (ButtonColorStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    AppSettingsRow(modifier = modifier) {
        Text(
            text = label,
            color = colors.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        AppDropdown(
            selected = selected,
            options = ButtonColorStyle.entries,
            optionText = { style ->
                when (style) {
                    ButtonColorStyle.ACCENTED -> stringResource(R.string.macropad_editor_button_color_accented)
                    ButtonColorStyle.NEUTRAL -> stringResource(R.string.macropad_editor_button_color_neutral)
                }
            },
            onSelected = onSelect,
        )
    }
}
