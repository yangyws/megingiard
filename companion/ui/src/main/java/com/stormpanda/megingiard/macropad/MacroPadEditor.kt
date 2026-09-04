package com.stormpanda.megingiard.macropad

import android.content.Context
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.ViewQuilt
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Grid4x4
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SmartButton
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.TableRows
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stormpanda.megingiard.AppLog
import com.stormpanda.megingiard.AppStateManager
import com.stormpanda.megingiard.BitmapUtils
import com.stormpanda.megingiard.CompanionViewMode
import com.stormpanda.megingiard.R
import com.stormpanda.megingiard.keyboard.LinuxKeycodes
import com.stormpanda.megingiard.privd.PrivdManager
import com.stormpanda.megingiard.privd.PrivdState
import com.stormpanda.megingiard.settings.MacroPadSettings
import com.stormpanda.megingiard.steamgriddb.SteamGridDbScrapeSubPageContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.stormpanda.megingiard.ui.AppDivider
import com.stormpanda.megingiard.ui.AppIcon
import com.stormpanda.megingiard.ui.BumperDirection
import com.stormpanda.megingiard.ui.DialogToastManager
import com.stormpanda.megingiard.ui.FullScreenTopBar
import com.stormpanda.megingiard.ui.GamepadActionCard
import com.stormpanda.megingiard.ui.GamepadCardRow
import com.stormpanda.megingiard.ui.GamepadCategoryTile
import com.stormpanda.megingiard.ui.GamepadChoiceCard
import com.stormpanda.megingiard.ui.GamepadDeck
import com.stormpanda.megingiard.ui.GamepadFocusCard
import com.stormpanda.megingiard.ui.GamepadInfoBox
import com.stormpanda.megingiard.ui.GamepadPill
import com.stormpanda.megingiard.ui.GamepadReorderCard
import com.stormpanda.megingiard.ui.GamepadReorderDeck
import com.stormpanda.megingiard.ui.GamepadSectionHeader
import com.stormpanda.megingiard.ui.GamepadStepperCard
import com.stormpanda.megingiard.ui.GamepadToggleCard
import com.stormpanda.megingiard.ui.GamepadTwoPaneScaffold
import com.stormpanda.megingiard.ui.GamepadTwoStepConfirmCard
import com.stormpanda.megingiard.ui.LocalAppColors
import com.stormpanda.megingiard.ui.PrimaryModalPayload
import com.stormpanda.megingiard.ui.PrimaryModalType
import com.stormpanda.megingiard.ui.PrimaryOverlayInputBridge
import com.stormpanda.megingiard.ui.cycle
import com.stormpanda.megingiard.ui.firstDeckItem
import com.stormpanda.megingiard.ui.handle2DAdjustmentKeyEvent
import com.stormpanda.megingiard.ui.launchDirectionalRepeat
import com.stormpanda.megingiard.ui.rememberGamepadBringIntoViewSpec
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.util.Collections
import java.util.UUID
import kotlin.math.max

private const val TAG = "MacroPadEditor"
private val MPE_DECK_SPACING = 10.dp
private val MPE_EMPTY_PADDING_V = 12.dp
private const val MPE_BUTTON_HEADER_COUNT = 5
private const val MPE_CANVAS_WIDTH_PX = 1240f
private const val MPE_CANVAS_HEIGHT_PX = 1080f
private const val MPE_MOVE_STEP_NORMALIZED = 0.015f
private const val MPE_EDGE_MARGIN = 0.05f

private fun EditorSection.titleResId(): Int =
    when (this) {
        EditorSection.QUICK_ACTIONS -> R.string.quick_actions_title
        EditorSection.PROFILES -> R.string.quick_menu_profile_label
        EditorSection.LAYOUTS -> R.string.macropad_editor_section_layout
        EditorSection.MIRROR -> R.string.quick_menu_screen_mirroring
        EditorSection.BACKGROUND -> R.string.layout_settings_bg_section_title
        EditorSection.BUTTONS -> R.string.macropad_editor_section_buttons
        EditorSection.MACROS -> R.string.macropad_editor_manage_macros
    }

private fun EditorSection.icon(): ImageVector =
    when (this) {
        EditorSection.QUICK_ACTIONS -> Icons.Rounded.Bolt
        EditorSection.PROFILES -> Icons.Rounded.Folder
        EditorSection.LAYOUTS -> Icons.AutoMirrored.Rounded.ViewQuilt
        EditorSection.MIRROR -> Icons.Rounded.Videocam
        EditorSection.BACKGROUND -> Icons.Rounded.Wallpaper
        EditorSection.BUTTONS -> Icons.Rounded.SmartButton
        EditorSection.MACROS -> Icons.AutoMirrored.Rounded.PlaylistPlay
    }

private fun applyActionToDraftButton(
    draftButton: PadButton,
    newAction: PadAction,
): PadButton =
    draftButton.copy(
        action = newAction,
        buttonSize = if (newAction is PadAction.ScrollWheel) ButtonSize.SIZE_1X2 else draftButton.buttonSize,
    )

private fun swapButtons(
    layout: PadLayout?,
    from: Int,
    to: Int,
) {
    if (layout == null || from !in layout.buttons.indices || to !in layout.buttons.indices) return
    val mutable = layout.buttons.toMutableList()
    Collections.swap(mutable, from, to)
    MacroPadState.updateLayout(layout.copy(buttons = mutable))
}

internal val MPE_PADDING = 16.dp

@Composable
fun MacroPadEditor(
    onDone: () -> Unit,
    showTopBar: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profiles by MacroPadState.profiles.collectAsStateWithLifecycle()
    val activeId by MacroPadState.activeProfileId.collectAsStateWithLifecycle()
    val colors = LocalAppColors.current
    val globalAccentColor = colors.accent

    DisposableEffect(Unit) {
        AppLog.i(TAG, "MacroPadEditor visible")
        onDispose {
            AppLog.i(TAG, "MacroPadEditor dismissed")
            MacroPadState.setEditingButtonPositions(false)
            MacroPadState.setSelectedButtonId(null)
            MacroPadState.clearPreviewLayout()
        }
    }

    val profile = profiles.firstOrNull { it.id == activeId } ?: profiles.firstOrNull()
    val activeLayout =
        remember(profile) {
            val layoutId = profile?.activeLayoutId
            profile?.layouts?.firstOrNull { it.id == layoutId } ?: profile?.layouts?.firstOrNull()
        }

    val selectedSection by MacroPadNavState.selectedSection.collectAsStateWithLifecycle()
    val subPageStack by MacroPadNavState.subPageStack.collectAsStateWithLifecycle()

    val macroTimelineFocusStepIndex by MacroPadNavState.macroTimelineFocusStepIndex.collectAsStateWithLifecycle()
    var appearanceDraft by remember { mutableStateOf<PadLayout?>(null) }
    var buttonDraft by remember { mutableStateOf<PadButton?>(null) }

    val activePrimaryModal by AppStateManager.activePrimaryModal.collectAsStateWithLifecycle()
    val savedFocusKeys by MacroPadNavState.savedFocusKeysByDepth.collectAsStateWithLifecycle()

    LaunchedEffect(selectedSection) {
        MacroPadState.setSelectedButtonId(null)
    }

    LaunchedEffect(activePrimaryModal) {
        MacroPadNavState.applyPrimaryModalPayload(activePrimaryModal?.payload)
    }

    LaunchedEffect(subPageStack) {
        val isEditingPositionsSubPage =
            subPageStack.any { it is MacroPadSubPage.EditButtonPositions }
        MacroPadState.setEditingButtonPositions(isEditingPositionsSubPage)

        val editButtonSubPage = subPageStack.filterIsInstance<MacroPadSubPage.EditButton>().lastOrNull()
        if (editButtonSubPage != null) {
            val btnId = editButtonSubPage.button?.id ?: editButtonSubPage.draftButton?.id ?: buttonDraft?.id
            if (btnId != null) {
                MacroPadState.setSelectedButtonId(btnId)
            }
        }
    }

    LaunchedEffect(subPageStack, selectedSection) {
        val hasAppearanceSubPages =
            subPageStack.any { it.parentSection == EditorSection.LAYOUTS }
        val hasButtonSubPages =
            subPageStack.any { it.parentSection == EditorSection.BUTTONS }
        val hasBackgroundSubPages =
            subPageStack.any { it.parentSection == EditorSection.BACKGROUND }
        val isBackgroundSection = selectedSection == EditorSection.BACKGROUND
        if (!hasAppearanceSubPages) {
            appearanceDraft = null
        }
        if (!hasButtonSubPages) {
            buttonDraft = null
            if (selectedSection != EditorSection.BUTTONS) {
                MacroPadState.setSelectedButtonId(null)
            }
        }
        if (!hasAppearanceSubPages && !hasButtonSubPages && !hasBackgroundSubPages && !isBackgroundSection) {
            MacroPadState.clearPreviewLayout()
        }
    }

    LaunchedEffect(buttonDraft) {
        if (buttonDraft != null) {
            MacroPadState.setPreviewButton(buttonDraft)
        }
    }

    BackHandler(enabled = true) {
        if (subPageStack.isNotEmpty()) {
            MacroPadNavState.pop()
        } else {
            MacroPadNavState.reset()
            onDone()
        }
    }

    LaunchedEffect(Unit) {
        PrimaryOverlayInputBridge.bumperEvents.collect { direction ->
            MacroPadNavState.selectSection(EditorSection.entries.cycle(selectedSection, direction))
        }
    }

    LaunchedEffect(Unit) {
        ButtonImagePickerManager.pickedUriFlow.collect { uri ->
            ButtonImagePickerManager.clearPickedUri()
            AppLog.i(TAG, "MacroPadEditor received picked button image uri: $uri")
            val decoded =
                withContext(Dispatchers.IO) {
                    BitmapUtils.decodeScaledBitmapFromUri(
                        context,
                        uri,
                        targetW = BTN_IMAGE_DECODE_PX,
                        targetH = BTN_IMAGE_DECODE_PX,
                    )
                }
            if (decoded != null) {
                val activeSubPage = subPageStack.lastOrNull()
                val parentDraft =
                    buttonDraft ?: subPageStack.filterIsInstance<MacroPadSubPage.EditButton>().lastOrNull()?.let {
                        it.draftButton ?: it.button
                    }
                val isGrid = activeLayout?.isGridMode == true || (activeSubPage as? MacroPadSubPage.ChooseButtonImage)?.isGridMode == true
                val (btn, draftBtn, aspect) =
                    when (activeSubPage) {
                        is MacroPadSubPage.ChooseButtonImage ->
                            Triple(activeSubPage.button, activeSubPage.draftButton ?: parentDraft, activeSubPage.aspectRatio)
                        is MacroPadSubPage.EditButton -> {
                            val target = activeSubPage.draftButton ?: activeSubPage.button
                            val aspect =
                                if (isGrid) {
                                    GridLayoutMath.cellAspectRatio(
                                        cols = activeLayout?.effectiveGridCols ?: 1,
                                        rows = activeLayout?.effectiveGridRows ?: 1,
                                        colSpan = target?.colSpan ?: 1,
                                        rowSpan = target?.rowSpan ?: 1,
                                    )
                                } else {
                                    ((target?.buttonSize?.cols ?: 1).toFloat() / (target?.buttonSize?.rows ?: 1).toFloat().coerceAtLeast(1f)).coerceAtLeast(0.1f)
                                }
                            Triple(activeSubPage.button, target, aspect)
                        }
                        else -> {
                            val aspect =
                                if (isGrid) {
                                    GridLayoutMath.cellAspectRatio(
                                        cols = activeLayout?.effectiveGridCols ?: 1,
                                        rows = activeLayout?.effectiveGridRows ?: 1,
                                        colSpan = parentDraft?.colSpan ?: 1,
                                        rowSpan = parentDraft?.rowSpan ?: 1,
                                    )
                                } else {
                                    ((parentDraft?.buttonSize?.cols ?: 1).toFloat() / (parentDraft?.buttonSize?.rows ?: 1).toFloat().coerceAtLeast(1f)).coerceAtLeast(0.1f)
                                }
                            Triple(null, parentDraft, aspect)
                        }
                    }
                if (draftBtn != null) {
                    val savedAssetId = PadIconStore.put(context, decoded)
                    if (savedAssetId != null) {
                        val updated =
                            draftBtn.copy(
                                imageAssetId = savedAssetId,
                                iconName = null,
                                imageScaleMode = BackgroundScaleMode.FILL,
                                imageScale = 1.0f,
                                imageOffsetX = 0f,
                                imageOffsetY = 0f,
                            )
                        buttonDraft = updated
                        MacroPadState.setPreviewButton(updated)
                        val targetStack =
                            subPageStack.filterNot {
                                it is MacroPadSubPage.ChooseButtonImage || it is MacroPadSubPage.ChooseIcon || it is MacroPadSubPage.CropButtonImage
                            }.map { subPage ->
                                if (subPage is MacroPadSubPage.EditButton) {
                                    subPage.copy(draftButton = updated)
                                } else if (subPage is MacroPadSubPage.ButtonCustomImage) {
                                    subPage.copy(draftButton = updated)
                                } else {
                                    subPage
                                }
                            }
                        MacroPadNavState.setStack(targetStack)
                    }
                }
            }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.appBackground),
    ) {
        if (showTopBar && subPageStack.isEmpty()) {
            FullScreenTopBar(
                title = stringResource(R.string.macropad_editor_title),
                onDismiss = {
                    MacroPadNavState.reset()
                    onDone()
                },
            )
            AppDivider()
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (profile == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.macropad_no_profile),
                        color = colors.onSurfaceSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(MPE_PADDING),
                    )
                }
            } else {
                GamepadTwoPaneScaffold(
                    modifier = Modifier.fillMaxSize(),
                    scrollableDeck = false,
                    isCustomBackActive = subPageStack.isNotEmpty(),
                    onCustomBack = {
                        MacroPadNavState.pop()
                    },
                    navigationKey = subPageStack,
                    savedFocusKeys = savedFocusKeys,
                    onRecordFocusedKey = { depth, key -> MacroPadNavState.recordFocusedKey(depth, key) },
                    onRemoveFocusedKey = { depth -> MacroPadNavState.removeFocusedKey(depth) },
                    sidebarContent = {
                        EditorSection.entries.forEach { section ->
                            GamepadCategoryTile(
                                title = stringResource(section.titleResId()),
                                icon = section.icon(),
                                selected = selectedSection == section,
                                onClick = {
                                    MacroPadNavState.selectSection(section)
                                },
                            )
                        }
                    },
                    content = {
                        AnimatedContent(
                            targetState = subPageStack,
                            transitionSpec = {
                                if (targetState.size < initialState.size) {
                                    slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> width } + fadeOut()
                                } else if (targetState.size > initialState.size) {
                                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> -width } + fadeOut()
                                } else {
                                    EnterTransition.None togetherWith ExitTransition.None
                                }
                            },
                            label = "MacroPadSubPageAnimation",
                            modifier = Modifier.fillMaxSize(),
                        ) { stack ->
                            val currentSubPage = stack.lastOrNull()
                            if (currentSubPage == null) {
                                val sectionTitle = stringResource(selectedSection.titleResId())
                                GamepadDeck(
                                    title = sectionTitle,
                                    scrollable = selectedSection != EditorSection.BUTTONS,
                                ) {
                                    // ── Main Section Decks ─────────────────────────────
                                    when (selectedSection) {
                                        EditorSection.QUICK_ACTIONS -> {
                                            QuickActionsDeckContent(
                                                onNewButton = {
                                                    val isGrid = activeLayout?.isGridMode == true
                                                    val freeCell = if (isGrid && activeLayout != null) GridLayoutMath.firstFreeCell(activeLayout) else null
                                                    val col = freeCell?.first
                                                    val row = freeCell?.second
                                                    MacroPadNavState.setStack(
                                                        listOf(
                                                            MacroPadSubPage.ChooseButtonType(
                                                                initialGridCol = col,
                                                                initialGridRow = row,
                                                            ),
                                                        ),
                                                    )
                                                },
                                                onNewMacro = {
                                                    MacroPadNavState.setMacroTimelineFocusStepIndex(null)
                                                    MacroPadNavState.setStack(
                                                        listOf(MacroPadSubPage.ChooseMacroMode),
                                                    )
                                                },
                                                onNewLayout = {
                                                    MacroPadNavState.setStack(listOf(MacroPadSubPage.NewLayout))
                                                },
                                                onNewProfile = {
                                                    MacroPadNavState.setStack(listOf(MacroPadSubPage.NewProfile()))
                                                },
                                                onArrangeButtons = {
                                                    MacroPadNavState.setStack(listOf(MacroPadSubPage.EditButtonPositions))
                                                },
                                                onEditMirrorLayout = {
                                                    onDone()
                                                    AppStateManager.setViewportEditActive(true)
                                                },
                                            )
                                        }

                                        EditorSection.PROFILES -> {
                                            ProfilesDeck(
                                                profiles = profiles,
                                                activeProfile = profile,
                                                accentColor = colors.accent,
                                                onSelectProfile = {
                                                    MacroPadState.setActiveProfileId(it)
                                                    AppStateManager.setCompanionViewMode(CompanionViewMode.MACROPAD)
                                                },
                                                onNewProfile = {
                                                    MacroPadNavState.push(MacroPadSubPage.NewProfile())
                                                },
                                                onEditProfile = {
                                                    MacroPadNavState.push(MacroPadSubPage.EditProfile(profile.id))
                                                },
                                                onDuplicateProfile = {
                                                    val originalProfile = profile
                                                    val originalLayouts = originalProfile.layouts
                                                    val layoutMapping = MacroPadState.duplicateProfile(originalProfile.id)
                                                    if (layoutMapping != null) {
                                                        for (origLayout in originalLayouts) {
                                                            val originalPath = origLayout.backgroundImagePath
                                                            val newLayoutId = layoutMapping[origLayout.id]
                                                            if (originalPath != null && newLayoutId != null) {
                                                                scope.launch {
                                                                    MacroPadMediaRepository.duplicateBackgroundImage(
                                                                        context,
                                                                        origLayout.id,
                                                                        newLayoutId,
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        val duplicatedProfile = MacroPadState.activeProfile.value
                                                        val duplicatedName = duplicatedProfile?.name ?: originalProfile.name
                                                        DialogToastManager.show(
                                                            context.getString(R.string.macropad_profile_duplicated_toast, duplicatedName),
                                                        )
                                                    }
                                                },
                                                onReorderProfiles = {
                                                    MacroPadNavState.push(MacroPadSubPage.ReorderProfiles)
                                                },
                                            )
                                        }

                                        EditorSection.LAYOUTS -> {
                                            LayoutsDeck(
                                                profile = profile,
                                                activeLayout = activeLayout,
                                                accentColor = colors.accent,
                                                onSelectLayout = {
                                                    MacroPadState.setActiveLayoutId(it)
                                                    AppStateManager.setCompanionViewMode(CompanionViewMode.MACROPAD)
                                                },
                                                onLayoutModeChange = { newMode ->
                                                    if (activeLayout != null) {
                                                        val updated = activeLayout.copy(layoutMode = newMode).withCellsAssigned()
                                                        MacroPadState.updateLayout(updated)
                                                        MacroPadState.setPreviewLayout(null)
                                                    }
                                                },
                                                onEditLayout = {
                                                    if (activeLayout != null) {
                                                        MacroPadNavState.push(MacroPadSubPage.EditLayout(activeLayout.id))
                                                    }
                                                },
                                                onNewLayout = {
                                                    MacroPadNavState.push(MacroPadSubPage.NewLayout)
                                                },
                                                onDuplicateLayout = {
                                                    val originalLayout = activeLayout
                                                    val originalPath = originalLayout?.backgroundImagePath
                                                    val newLayoutId = originalLayout?.id?.let { MacroPadState.duplicateLayout(it) }
                                                    if (originalLayout != null && newLayoutId != null) {
                                                        if (originalPath != null) {
                                                            scope.launch {
                                                                MacroPadMediaRepository.duplicateBackgroundImage(
                                                                    context,
                                                                    originalLayout.id,
                                                                    newLayoutId,
                                                                )
                                                            }
                                                        }
                                                        val duplicatedLayout =
                                                            MacroPadState.activeProfile.value?.layouts?.firstOrNull {
                                                                it.id ==
                                                                    newLayoutId
                                                            }
                                                        val duplicatedName = duplicatedLayout?.name ?: originalLayout.name
                                                        DialogToastManager.show(
                                                            context.getString(R.string.macropad_layout_duplicated_toast, duplicatedName),
                                                        )
                                                    }
                                                },
                                                onCopyLayout = {
                                                    if (activeLayout != null) {
                                                        MacroPadNavState.push(MacroPadSubPage.CopyLayout(activeLayout.id))
                                                    }
                                                },
                                                onReorderLayouts = {
                                                    MacroPadNavState.push(MacroPadSubPage.ReorderLayouts)
                                                },
                                            )
                                        }

                                        EditorSection.MIRROR -> {
                                            if (activeLayout != null) {
                                                MirrorDeck(
                                                    profile = profile,
                                                    layout = activeLayout,
                                                    accentColor = colors.accent,
                                                    onArrangeCutouts = {
                                                        onDone()
                                                        AppStateManager.setViewportEditActive(true)
                                                    },
                                                    onOpenAdvancedSettings = {
                                                        MacroPadNavState.push(MacroPadSubPage.MirrorAdvancedSettings(activeLayout.id))
                                                    },
                                                    onEditCutout = { cutout ->
                                                        MacroPadNavState.push(MacroPadSubPage.CutoutSettings(cutout.id))
                                                    },
                                                )
                                            }
                                        }

                                        EditorSection.BACKGROUND -> {
                                            if (activeLayout != null) {
                                                LayoutBackgroundSubPageContent(
                                                    layout = activeLayout,
                                                    profileName = profile.name,
                                                    accentColor = colors.accent,
                                                    onOpenScrape = {
                                                        MacroPadNavState.push(MacroPadSubPage.SteamGridDbScrape(activeLayout.id))
                                                    },
                                                    onOpenPicker = {
                                                        MacroPadNavState.push(MacroPadSubPage.ChooseBackgroundImage(activeLayout.id))
                                                    },
                                                    onDiscard = {
                                                        MacroPadState.clearPreviewLayout()
                                                    },
                                                    onConfirm = {
                                                        bgImagePath,
                                                        useAsMask,
                                                        bgChanged,
                                                        bgScale,
                                                        bgOffsetX,
                                                        bgOffsetY,
                                                        bgDim,
                                                        bgScaleMode,
                                                        ->
                                                        MacroPadState.clearPreviewLayout()
                                                        MacroPadState.updateLayout(
                                                            activeLayout.copy(
                                                                backgroundImagePath = bgImagePath,
                                                                useBackgroundImageAsMask = useAsMask,
                                                                backgroundImageVersion =
                                                                    if (bgChanged) activeLayout.backgroundImageVersion + 1 else activeLayout.backgroundImageVersion,
                                                                bgImageScale = bgScale,
                                                                bgImageOffsetX = bgOffsetX,
                                                                bgImageOffsetY = bgOffsetY,
                                                                backgroundImageDim = bgDim,
                                                                bgScaleMode = bgScaleMode,
                                                            ),
                                                        )
                                                        DialogToastManager.show(
                                                            context.getString(R.string.gamepad_action_save_and_exit_desc),
                                                        )
                                                    },
                                                )
                                            }
                                        }

                                        EditorSection.BUTTONS -> {
                                            ButtonsDeck(
                                                profile = profile,
                                                layout = activeLayout,
                                                accentColor = colors.accent,
                                                onEditButtonPositions = {
                                                    MacroPadNavState.push(MacroPadSubPage.EditButtonPositions)
                                                },
                                                onAddButton = {
                                                    val isGrid = activeLayout?.isGridMode == true
                                                    val freeCell = if (isGrid && activeLayout != null) GridLayoutMath.firstFreeCell(activeLayout) else null
                                                    val col = freeCell?.first
                                                    val row = freeCell?.second
                                                    MacroPadNavState.push(
                                                        MacroPadSubPage.ChooseButtonType(
                                                            initialGridCol = col,
                                                            initialGridRow = row,
                                                        ),
                                                    )
                                                },
                                                onEditButton = { btn ->
                                                    MacroPadNavState.push(MacroPadSubPage.EditButton(btn))
                                                },
                                            )
                                        }

                                        EditorSection.MACROS -> {
                                            MacrosDeck(
                                                profile = profile,
                                                accentColor = colors.accent,
                                                onNewMacro = {
                                                    if (PrivdManager.state.value != PrivdState.RUNNING) {
                                                        DialogToastManager.show(context.getString(R.string.privd_error_daemon_unreachable))
                                                    } else {
                                                        MacroPadNavState.push(MacroPadSubPage.ChooseMacroMode)
                                                    }
                                                },
                                                onEditMacro = { macro ->
                                                    MacroPadNavState.push(MacroPadSubPage.MacroTimeline(macro = macro))
                                                },
                                                onDeleteMacro = { macro ->
                                                    val deletedName = macro.name
                                                    MacroPadState.deleteMacro(macro.id)
                                                    DialogToastManager.show(
                                                        context.getString(R.string.macropad_macro_deleted_toast, deletedName),
                                                    )
                                                },
                                            )
                                        }
                                    }
                                }
                            } else {
                                // ── In-Deck Sub-Pages ──────────────────────────────
                                fun updateStackDraftMacro(
                                    stack: List<MacroPadSubPage>,
                                    updatedMacro: Macro,
                                ): List<MacroPadSubPage> =
                                    stack.map { page ->
                                        when (page) {
                                            is MacroPadSubPage.MacroTimeline -> page.copy(draftMacro = updatedMacro)
                                            is MacroPadSubPage.ManualMacroSteps -> page.copy(draftMacro = updatedMacro)
                                            is MacroPadSubPage.ReorderMacroSteps -> page.copy(draftMacro = updatedMacro)
                                            else -> page
                                        }
                                    }

                                fun updateParentDraftButton(updatedDraft: PadButton?) {
                                    buttonDraft = updatedDraft
                                    MacroPadNavState.setStack(
                                        subPageStack.dropLast(1).map { subPage ->
                                            if (subPage is MacroPadSubPage.EditButton) {
                                                subPage.copy(draftButton = updatedDraft)
                                            } else {
                                                subPage
                                            }
                                        },
                                    )
                                }

                                @Composable
                                fun buttonBreadcrumbs(
                                    effectiveButton: PadButton,
                                    hasButton: Boolean,
                                    title: String,
                                ): List<String> =
                                    listOf(
                                        stringResource(R.string.macropad_editor_section_buttons),
                                        effectiveButton.label.ifBlank {
                                            stringResource(
                                                if (hasButton) {
                                                    R.string.macropad_editor_section_button_settings
                                                } else {
                                                    R.string.macropad_editor_add_button
                                                },
                                            )
                                        },
                                        title,
                                    )

                                when (currentSubPage) {
                                    is MacroPadSubPage.NewProfile -> {
                                        GamepadDeck(
                                            breadcrumbs =
                                                listOf(
                                                    stringResource(R.string.quick_menu_profile_label),
                                                    stringResource(R.string.settings_macropad_new_profile),
                                                ),
                                        ) {
                                            NewProfileSubPageContent(
                                                existingNames = profiles.map { it.name },
                                                accentColor = colors.accent,
                                                presetName = currentSubPage.presetName,
                                                onDiscard = { MacroPadNavState.pop() },
                                                onCreate = { name ->
                                                    val newId = UUID.randomUUID().toString()
                                                    val defaultLayoutId = UUID.randomUUID().toString()
                                                    val newProf =
                                                        PadProfile(
                                                            id = newId,
                                                            name = name,
                                                            association = currentSubPage.association,
                                                            layouts =
                                                                listOf(
                                                                    PadLayout(
                                                                        id = defaultLayoutId,
                                                                        name = context.getString(R.string.integration_home_default_layout),
                                                                    ),
                                                                ),
                                                            activeLayoutId = defaultLayoutId,
                                                        )
                                                    MacroPadState.addProfile(newProf)
                                                    MacroPadState.setActiveProfileId(newId)
                                                    MacroPadNavState.selectSection(EditorSection.PROFILES)
                                                    MacroPadNavState.setStack(emptyList())
                                                },
                                            )
                                        }
                                    }

                                    is MacroPadSubPage.EditProfile -> {
                                        val prof = profiles.firstOrNull { it.id == currentSubPage.profileId } ?: profile
                                        GamepadDeck(
                                            breadcrumbs =
                                                listOf(
                                                    stringResource(R.string.quick_menu_profile_label),
                                                    stringResource(R.string.macropad_editor_edit_profile_title),
                                                ),
                                        ) {
                                            EditProfileSubPageContent(
                                                profile = prof,
                                                existingNames = profiles.filter { it.id != prof.id }.map { it.name },
                                                accentColor = colors.accent,
                                                onNameChange = { name ->
                                                    MacroPadState.renameProfile(prof.id, name)
                                                },
                                                onUnlinkApp = {
                                                    val unlinked = prof.copy(association = null)
                                                    MacroPadState.updateProfile(unlinked)
                                                    DialogToastManager.show(
                                                        context.getString(R.string.macropad_profile_unlinked_toast, prof.name),
                                                    )
                                                },
                                                onDeleteProfile = {
                                                    val deletedName = prof.name
                                                    val layoutsToDelete = prof.layouts
                                                    scope.launch {
                                                        layoutsToDelete.forEach { lay ->
                                                            MacroPadMediaRepository.deleteBackgroundImage(context, lay.id)
                                                        }
                                                    }
                                                    MacroPadState.deleteProfile(prof.id)
                                                    MacroPadNavState.pop()
                                                    DialogToastManager.show(
                                                        context.getString(R.string.macropad_profile_deleted_toast, deletedName),
                                                    )
                                                },
                                            )
                                        }
                                    }

                                    is MacroPadSubPage.AppPicker -> {
                                        GamepadDeck(
                                            breadcrumbs =
                                                listOf(
                                                    stringResource(R.string.macropad_editor_section_buttons),
                                                    stringResource(R.string.app_launcher_picker_title),
                                                ),
                                            scrollable = false,
                                        ) {
                                            AppPickerSubPageContent(
                                                assignedPackages = emptySet(),
                                                accentColor = colors.accent,
                                                onSelectApp = { pkg ->
                                                    MacroPadNavState.setStack(
                                                        subPageStack.dropLast(1).map { subPage ->
                                                            if (subPage is MacroPadSubPage.EditButton) {
                                                                val draft =
                                                                    buttonDraft
                                                                        ?: subPage.draftButton
                                                                        ?: subPage.button
                                                                        ?: PadButton(
                                                                            id = UUID.randomUUID().toString(),
                                                                            label =
                                                                                context.getString(
                                                                                    R.string.macropad_editor_new_button_default_label,
                                                                                ),
                                                                            posX = 0.5f,
                                                                            posY = 0.5f,
                                                                            action = PadAction.AppLauncher(pkg),
                                                                        )
                                                                val newBtn = draft.copy(action = PadAction.AppLauncher(pkg))
                                                                buttonDraft = newBtn
                                                                subPage.copy(draftButton = newBtn)
                                                            } else {
                                                                subPage
                                                            }
                                                        },
                                                    )
                                                },
                                            )
                                        }
                                    }

                                    is MacroPadSubPage.ReorderProfiles -> {
                                        ReorderProfilesSubPage(
                                            profiles = profiles,
                                        )
                                    }

                                    is MacroPadSubPage.NewLayout -> {
                                        GamepadDeck(
                                            breadcrumbs =
                                                listOf(
                                                    stringResource(R.string.macropad_editor_section_layout),
                                                    stringResource(R.string.settings_macropad_new_layout),
                                                ),
                                        ) {
                                            NewLayoutSubPageContent(
                                                existingNames = profile.layouts.map { it.name },
                                                accentColor = colors.accent,
                                                onDiscard = { MacroPadNavState.pop() },
                                                onCreate = { name ->
                                                    val newId = UUID.randomUUID().toString()
                                                    val newLayout =
                                                        PadLayout(
                                                            id = newId,
                                                            name = name,
                                                            enabled = true,
                                                        )
                                                    MacroPadState.addLayout(newLayout)
                                                    MacroPadNavState.setStack(listOf(MacroPadSubPage.EditLayout(newId)))
                                                },
                                            )
                                        }
                                    }

                                    is MacroPadSubPage.EditLayout -> {
                                        val lay = profile.layouts.firstOrNull { it.id == currentSubPage.layoutId } ?: activeLayout
                                        if (lay != null) {
                                            val currentDraft = appearanceDraft?.takeIf { it.id == lay.id } ?: lay
                                            GamepadDeck(
                                                breadcrumbs =
                                                    listOf(
                                                        stringResource(R.string.macropad_editor_section_layout),
                                                        stringResource(R.string.macropad_editor_edit_layout_title),
                                                    ),
                                            ) {
                                                EditLayoutSubPageContent(
                                                    layout = currentDraft,
                                                    savedLayout = lay,
                                                    existingNames = profile.layouts.filter { it.id != lay.id }.map { it.name },
                                                    accentColor = colors.accent,
                                                    onNameChange = { newName ->
                                                        val base = appearanceDraft?.takeIf { it.id == lay.id } ?: lay
                                                        val updated = base.copy(name = newName)
                                                        MacroPadState.updateLayout(updated)
                                                        MacroPadState.setPreviewLayout(updated)
                                                        appearanceDraft = updated
                                                    },
                                                    onGridColsChange = { newCols ->
                                                        val base = appearanceDraft?.takeIf { it.id == lay.id } ?: lay
                                                        val updated = base.copy(gridCols = newCols).withCellsAssigned()
                                                        MacroPadState.updateLayout(updated)
                                                        MacroPadState.setPreviewLayout(updated)
                                                        appearanceDraft = updated
                                                    },
                                                    onGridRowsChange = { newRows ->
                                                        val base = appearanceDraft?.takeIf { it.id == lay.id } ?: lay
                                                        val updated = base.copy(gridRows = newRows).withCellsAssigned()
                                                        MacroPadState.updateLayout(updated)
                                                        MacroPadState.setPreviewLayout(updated)
                                                        appearanceDraft = updated
                                                    },
                                                    onGridShowBordersChange = { newShowBorders ->
                                                        val base = appearanceDraft?.takeIf { it.id == lay.id } ?: lay
                                                        val updated = base.copy(gridShowBorders = newShowBorders)
                                                        MacroPadState.updateLayout(updated)
                                                        MacroPadState.setPreviewLayout(updated)
                                                        appearanceDraft = updated
                                                    },
                                                    onGridShowButtonBgChange = { newShowButtonBg ->
                                                        val base = appearanceDraft?.takeIf { it.id == lay.id } ?: lay
                                                        val updated = base.copy(gridShowButtonBg = newShowButtonBg)
                                                        MacroPadState.updateLayout(updated)
                                                        MacroPadState.setPreviewLayout(updated)
                                                        appearanceDraft = updated
                                                    },
                                                    onInvisibleButtonsChange = { newInvisible ->
                                                        val base = appearanceDraft?.takeIf { it.id == lay.id } ?: lay
                                                        val updated = base.copy(invisibleButtons = newInvisible)
                                                        MacroPadState.updateLayout(updated)
                                                        MacroPadState.setPreviewLayout(updated)
                                                        appearanceDraft = updated
                                                    },
                                                    onOpenColorSubMenu = { target ->
                                                        MacroPadNavState.push(MacroPadSubPage.LayoutColor(lay.id, target))
                                                    },
                                                    onOpenTouchpadSettings = {
                                                        MacroPadNavState.push(MacroPadSubPage.LayoutTouchpad(lay.id))
                                                    },
                                                    onDeleteLayout = {
                                                        val deletedName = lay.name
                                                        val isDeleted = MacroPadState.deleteLayout(lay.id)
                                                        if (isDeleted) {
                                                            scope.launch {
                                                                MacroPadMediaRepository.deleteBackgroundImage(context, lay.id)
                                                            }
                                                            appearanceDraft = null
                                                            MacroPadNavState.pop()
                                                            DialogToastManager.show(
                                                                context.getString(R.string.macropad_layout_deleted_toast, deletedName),
                                                            )
                                                        } else {
                                                            DialogToastManager.show(
                                                                context.getString(R.string.macropad_layout_cannot_delete_last_toast),
                                                            )
                                                        }
                                                    },
                                                    onDiscard = {
                                                        MacroPadState.updateLayout(lay)
                                                        appearanceDraft = null
                                                        MacroPadNavState.pop()
                                                    },
                                                    onSaveColors = { textCol, borderCol, bgCol ->
                                                        val base = appearanceDraft?.takeIf { it.id == lay.id } ?: lay
                                                        val updated =
                                                            base.copy(
                                                                buttonTextColor = textCol,
                                                                buttonBorderColor = borderCol,
                                                                buttonBgColor = bgCol,
                                                            )
                                                        MacroPadState.updateLayout(updated)
                                                        appearanceDraft = null
                                                        MacroPadNavState.pop()
                                                    },
                                                )
                                            }
                                        }
                                    }

                                    is MacroPadSubPage.LayoutColor -> {
                                        val lay = profile.layouts.firstOrNull { it.id == currentSubPage.layoutId } ?: activeLayout
                                        if (lay != null) {
                                            val currentDraft = appearanceDraft?.takeIf { it.id == lay.id } ?: lay
                                            GamepadDeck(
                                                breadcrumbs =
                                                    listOf(
                                                        stringResource(R.string.macropad_editor_section_layout),
                                                        stringResource(R.string.macropad_editor_edit_layout_title),
                                                        stringResource(currentSubPage.target.titleResId),
                                                    ),
                                            ) {
                                                LayoutColorSubPageContent(
                                                    layout = currentDraft,
                                                    savedLayout = lay,
                                                    target = currentSubPage.target,
                                                    accentColor = colors.accent,
                                                    onColorOptionChanged = { option ->
                                                        val updatedDraft = currentDraft.withColorOption(currentSubPage.target, option)
                                                        appearanceDraft = updatedDraft
                                                        MacroPadState.updateLayout(updatedDraft)
                                                        MacroPadState.setPreviewLayout(updatedDraft)
                                                    },
                                                    onOpenColorWheel = { title, breadcrumbs, initialColor, inFlightLayout ->
                                                        MacroPadNavState.push(
                                                            MacroPadSubPage.ColorWheel(
                                                                title = title,
                                                                breadcrumbs = breadcrumbs,
                                                                initialColor = initialColor,
                                                                section = EditorSection.LAYOUTS,
                                                                onColorChange = { liveColor ->
                                                                    val option = ColorOption.Custom(liveColor.toArgb())
                                                                    val base = appearanceDraft ?: inFlightLayout
                                                                    val liveLayout =
                                                                        base.withColorOption(
                                                                            currentSubPage.target,
                                                                            option,
                                                                        )
                                                                    appearanceDraft = liveLayout
                                                                    MacroPadState.updateLayout(liveLayout)
                                                                    MacroPadState.setPreviewLayout(liveLayout)
                                                                },
                                                            ),
                                                        )
                                                    },
                                                )
                                            }
                                        }
                                    }

                                    is MacroPadSubPage.MirrorAdvancedSettings -> {
                                        val lay = profile.layouts.firstOrNull { it.id == currentSubPage.layoutId } ?: activeLayout
                                        if (lay != null) {
                                            GamepadDeck(
                                                breadcrumbs =
                                                    listOf(
                                                        stringResource(R.string.quick_menu_screen_mirroring),
                                                        stringResource(R.string.settings_mirror_advanced_title),
                                                    ),
                                            ) {
                                                MirrorAdvancedSettingsSubPageContent(
                                                    layout = lay,
                                                    accentColor = colors.accent,
                                                )
                                            }
                                        }
                                    }

                                    is MacroPadSubPage.CutoutSettings -> {
                                        val layout = activeLayout
                                        val cutout =
                                            layout?.mirrorCutouts?.firstOrNull { it.id == currentSubPage.cutoutId }
                                        if (cutout != null) {
                                            val cutoutTitle =
                                                cutout.name.ifBlank {
                                                    val index = layout.mirrorCutouts.indexOfFirst { it.id == cutout.id }
                                                    stringResource(
                                                        R.string.settings_mirror_cutout_default_name_fmt,
                                                        if (index >= 0) index + 1 else 1,
                                                    )
                                                }
                                            GamepadDeck(
                                                breadcrumbs =
                                                    listOf(
                                                        stringResource(R.string.quick_menu_screen_mirroring),
                                                        cutoutTitle,
                                                    ),
                                            ) {
                                                CutoutSettingsSubPageContent(
                                                    cutout = cutout,
                                                    layout = layout,
                                                    accentColor = colors.accent,
                                                    onUpdateCutout = { updatedCutout, disableTouchpad ->
                                                        val updatedList =
                                                            layout.mirrorCutouts.map {
                                                                if (it.id == updatedCutout.id) updatedCutout else it
                                                            }
                                                        val updatedLayout =
                                                            if (disableTouchpad) {
                                                                layout.copy(
                                                                    mirrorCutouts = updatedList,
                                                                    backgroundTouchpad = layout.backgroundTouchpad.copy(enabled = false),
                                                                )
                                                            } else {
                                                                layout.copy(mirrorCutouts = updatedList)
                                                            }
                                                        MacroPadState.updateLayout(updatedLayout)
                                                    },
                                                    onDeleteCutout = { cutoutIdToDelete ->
                                                        val deletedName = cutout.name.ifBlank { cutoutTitle }
                                                        val updatedList = layout.mirrorCutouts.filter { it.id != cutoutIdToDelete }
                                                        MacroPadState.updateLayout(layout.copy(mirrorCutouts = updatedList))
                                                        DialogToastManager.show(
                                                            context.getString(R.string.macropad_cutout_deleted_toast, deletedName),
                                                        )
                                                        MacroPadNavState.pop()
                                                    },
                                                )
                                            }
                                        }
                                    }

                                    is MacroPadSubPage.ChooseBackgroundImage -> {
                                        val lay = profile.layouts.firstOrNull { it.id == currentSubPage.layoutId } ?: activeLayout
                                        if (lay != null) {
                                            GamepadDeck(
                                                breadcrumbs =
                                                    listOf(
                                                        stringResource(R.string.layout_settings_bg_section_title),
                                                        stringResource(R.string.button_image_picker_title),
                                                    ),
                                            ) {
                                                LocalImagePickerSubPageContent(
                                                    onSelectImage = { uri ->
                                                        BackgroundPickerManager.setPickedUri(uri)
                                                        MacroPadNavState.pop()
                                                    },
                                                    onOpenSteamGridDb = {
                                                        MacroPadNavState.push(
                                                            MacroPadSubPage.SteamGridDbScrape(layoutId = lay.id),
                                                        )
                                                    },
                                                    onOpenSystemPicker = {
                                                        BackgroundPickerManager.requestImagePicker()
                                                    },
                                                )
                                            }
                                        }
                                    }

                                    is MacroPadSubPage.SteamGridDbScrape -> {
                                        val isForButton = currentSubPage.targetButton != null || currentSubPage.draftButton != null
                                        val breadcrumbs =
                                            if (isForButton) {
                                                listOf(
                                                    stringResource(R.string.macropad_editor_section_buttons),
                                                    stringResource(R.string.layout_settings_bg_image_scrape),
                                                )
                                            } else {
                                                listOf(
                                                    stringResource(R.string.layout_settings_bg_section_title),
                                                    stringResource(R.string.layout_settings_bg_image_scrape),
                                                )
                                            }
                                        GamepadDeck(
                                            breadcrumbs = breadcrumbs,
                                        ) {
                                            SteamGridDbScrapeSubPageContent(
                                                initialSearchQuery = profile.name,
                                                accentColor = colors.accent,
                                                onImageSelected = { uri ->
                                                    if (isForButton) {
                                                        scope.launch {
                                                            val decoded =
                                                                withContext(Dispatchers.IO) {
                                                                    BitmapUtils.decodeScaledBitmapFromUri(
                                                                        context,
                                                                        uri,
                                                                        targetW = BTN_IMAGE_DECODE_PX,
                                                                        targetH = BTN_IMAGE_DECODE_PX,
                                                                    )
                                                                }
                                                            val targetDraft = currentSubPage.draftButton ?: currentSubPage.targetButton
                                                            if (decoded != null && targetDraft != null) {
                                                                val savedAssetId = PadIconStore.put(context, decoded)
                                                                if (savedAssetId != null) {
                                                                    val updated =
                                                                        targetDraft.copy(
                                                                            imageAssetId = savedAssetId,
                                                                            iconName = null,
                                                                            imageScaleMode = BackgroundScaleMode.FILL,
                                                                            imageScale = 1.0f,
                                                                            imageOffsetX = 0f,
                                                                            imageOffsetY = 0f,
                                                                        )
                                                                    buttonDraft = updated
                                                                    MacroPadState.setPreviewButton(updated)
                                                                    val targetStack =
                                                                        subPageStack.filterNot {
                                                                            it is MacroPadSubPage.SteamGridDbScrape || it is MacroPadSubPage.ChooseButtonImage || it is MacroPadSubPage.ChooseIcon || it is MacroPadSubPage.CropButtonImage
                                                                        }.map { subPage ->
                                                                            if (subPage is MacroPadSubPage.EditButton) {
                                                                                subPage.copy(draftButton = updated)
                                                                            } else if (subPage is MacroPadSubPage.ButtonCustomImage) {
                                                                                subPage.copy(draftButton = updated)
                                                                            } else {
                                                                                subPage
                                                                            }
                                                                        }
                                                                    MacroPadNavState.setStack(targetStack)
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        BackgroundPickerManager.setPickedUri(uri)
                                                        MacroPadNavState.pop()
                                                    }
                                                },
                                                onDiscard = {
                                                    MacroPadNavState.pop()
                                                },
                                            )
                                        }
                                    }

                                    is MacroPadSubPage.LayoutTouchpad -> {
                                        val lay = profile.layouts.firstOrNull { it.id == currentSubPage.layoutId } ?: activeLayout
                                        if (lay != null) {
                                            GamepadDeck(
                                                breadcrumbs =
                                                    listOf(
                                                        stringResource(R.string.macropad_editor_section_layout),
                                                        stringResource(R.string.macropad_editor_edit_layout_title),
                                                        stringResource(R.string.settings_touchpad_title),
                                                    ),
                                            ) {
                                                LayoutTouchpadSubPageContent(
                                                    layout = lay,
                                                    accentColor = colors.accent,
                                                    onUpdate = { updatedConfig, disableProjection ->
                                                        val newCutouts =
                                                            if (disableProjection) {
                                                                lay.mirrorCutouts.map { it.copy(touchProjectionEnabled = false) }
                                                            } else {
                                                                lay.mirrorCutouts
                                                            }
                                                        MacroPadState.updateLayout(
                                                            lay.copy(
                                                                backgroundTouchpad = updatedConfig,
                                                                mirrorCutouts = newCutouts,
                                                            ),
                                                        )
                                                    },
                                                )
                                            }
                                        }
                                    }

                                    is MacroPadSubPage.CopyLayout -> {
                                        val lay = profile.layouts.firstOrNull { it.id == currentSubPage.layoutId } ?: activeLayout
                                        if (lay != null) {
                                            GamepadDeck(
                                                breadcrumbs =
                                                    listOf(
                                                        stringResource(R.string.macropad_editor_section_layout),
                                                        stringResource(R.string.macropad_editor_copy_profile_select),
                                                    ),
                                            ) {
                                                CopyLayoutSubPageContent(
                                                    title = stringResource(R.string.macropad_editor_copy_profile_select),
                                                    profiles = profiles,
                                                    excludeProfileId = profile.id,
                                                    accentColor = colors.accent,
                                                    onSelect = { targetProfileId ->
                                                        MacroPadState.copyLayoutToProfile(lay, profile.id, targetProfileId)
                                                        MacroPadNavState.pop()
                                                        DialogToastManager.show(
                                                            context.getString(R.string.macropad_layout_copied_toast),
                                                        )
                                                    },
                                                )
                                            }
                                        }
                                    }

                                    is MacroPadSubPage.ReorderLayouts -> {
                                        ReorderLayoutsSubPage(
                                            layouts = profile.layouts,
                                        )
                                    }

                                    is MacroPadSubPage.EditButtonPositions -> {
                                        GamepadDeck(
                                            breadcrumbs =
                                                listOf(
                                                    stringResource(R.string.macropad_editor_section_buttons),
                                                    stringResource(R.string.macropad_editor_edit_button_positions),
                                                ),
                                        ) {
                                            EditButtonPositionsSubPageContent(
                                                layout = activeLayout,
                                                        accentColor = colors.accent,
                                                    )
                                                }
                                            }

                                    is MacroPadSubPage.ChooseButtonType -> {
                                        val isChangingType = currentSubPage.editingButton != null
                                        GamepadDeck(
                                            breadcrumbs =
                                                if (isChangingType) {
                                                    listOf(
                                                        stringResource(R.string.macropad_editor_section_buttons),
                                                        stringResource(R.string.macropad_editor_change_button_type),
                                                    )
                                                } else {
                                                    listOf(
                                                        stringResource(R.string.macropad_editor_section_buttons),
                                                        stringResource(R.string.macropad_editor_add_button),
                                                        stringResource(R.string.macropad_editor_button_type),
                                                    )
                                                },
                                        ) {
                                            ChooseButtonTypeSubPageContent(
                                                onSelectType = { group ->
                                                    if (group == ActionGroup.MACRO && PrivdManager.state.value != PrivdState.RUNNING) {
                                                        DialogToastManager.show(context.getString(R.string.privd_error_daemon_unreachable))
                                                        return@ChooseButtonTypeSubPageContent
                                                    }
                                                    val hasMacros = profile.macros.isNotEmpty()
                                                    val defaultCategory =
                                                        group.actions().firstOrNull { it.isEnabled(true, true, true, hasMacros) }
                                                            ?: group.actions().first()
                                                    val defaultAction = defaultCategory.defaultAction()
                                                    if (isChangingType) {
                                                        val existing = currentSubPage.editingButton!!
                                                        val updatedDraft = existing.copy(action = defaultAction)
                                                        buttonDraft = updatedDraft
                                                        MacroPadNavState.setStack(
                                                            subPageStack.dropLast(1) +
                                                                MacroPadSubPage.EditButton(
                                                                    button = currentSubPage.savedButton,
                                                                    draftButton = updatedDraft,
                                                                ),
                                                        )
                                                    } else {
                                                        val isGrid = activeLayout?.isGridMode == true
                                                        val col = currentSubPage.initialGridCol ?: if (isGrid && activeLayout != null) GridLayoutMath.firstFreeCell(activeLayout)?.first ?: 0 else null
                                                        val row = currentSubPage.initialGridRow ?: if (isGrid && activeLayout != null) GridLayoutMath.firstFreeCell(activeLayout)?.second ?: 0 else null
                                                        val newDraft =
                                                            PadButton(
                                                                id = UUID.randomUUID().toString(),
                                                                label = context.getString(R.string.macropad_editor_new_button_default_label),
                                                                posX = 0.5f,
                                                                posY = 0.5f,
                                                                gridCol = col,
                                                                gridRow = row,
                                                                colSpan = 1,
                                                                rowSpan = 1,
                                                                action = defaultAction,
                                                            )
                                                        buttonDraft = newDraft
                                                        MacroPadNavState.setStack(
                                                            subPageStack.dropLast(1) +
                                                                MacroPadSubPage.EditButton(
                                                                    button = null,
                                                                    draftButton = newDraft,
                                                                ),
                                                        )
                                                    }
                                                },
                                            )
                                        }
                                    }

                                    is MacroPadSubPage.EditButton -> {
                                        fun pushSubPageFromEdit(
                                            currentDraft: PadButton,
                                            subPage: MacroPadSubPage,
                                        ) {
                                            buttonDraft = currentDraft
                                            MacroPadNavState.setStack(
                                                subPageStack.dropLast(1) +
                                                    MacroPadSubPage.EditButton(
                                                        button = currentSubPage.button,
                                                        draftButton = currentDraft,
                                                    ) + subPage,
                                            )
                                        }
                                        val effectiveButton =
                                            buttonDraft?.takeIf { it.id == (currentSubPage.button?.id ?: currentSubPage.draftButton?.id) }
                                                ?: currentSubPage.draftButton
                                                ?: currentSubPage.button
                                                ?: PadButton(
                                                    id = UUID.randomUUID().toString(),
                                                    label = stringResource(R.string.macropad_editor_new_button_default_label),
                                                    posX = 0.5f,
                                                    posY = 0.5f,
                                                    action = PadAction.GamepadButton(GamepadKeycodes.BTN_SOUTH, "A"),
                                                )
                                        val isNew = currentSubPage.button == null
                                        GamepadDeck(
                                            breadcrumbs =
                                                listOf(
                                                    stringResource(R.string.macropad_editor_section_buttons),
                                                    stringResource(
                                                        if (!isNew) {
                                                            R.string.macropad_editor_section_button_settings
                                                        } else {
                                                            R.string.macropad_editor_add_button
                                                        },
                                                    ),
                                                ),
                                        ) {
                                            EditButtonSubPageContent(
                                                button = effectiveButton,
                                                savedButton = currentSubPage.button,
                                                accentColor = globalAccentColor,
                                                onOpenIconPicker = { currentDraft ->
                                                    pushSubPageFromEdit(currentDraft, MacroPadSubPage.ChooseIcon)
                                                },
                                                onOpenCustomImage = { currentDraft ->
                                                    pushSubPageFromEdit(
                                                        currentDraft,
                                                        MacroPadSubPage.ButtonCustomImage(
                                                            button = currentSubPage.button,
                                                            draftButton = currentDraft,
                                                        ),
                                                    )
                                                },
                                                onOpenAppPicker = { currentDraft ->
                                                    pushSubPageFromEdit(currentDraft, MacroPadSubPage.AppPicker)
                                                },
                                                onOpenColorSubMenu = { currentDraft, target ->
                                                    pushSubPageFromEdit(
                                                        currentDraft,
                                                        MacroPadSubPage.ButtonColor(
                                                            button = currentSubPage.button,
                                                            draftButton = currentDraft,
                                                            target = target,
                                                        ),
                                                    )
                                                },
                                                onOpenChooseButtonType = { currentDraft ->
                                                    pushSubPageFromEdit(
                                                        currentDraft,
                                                        MacroPadSubPage.ChooseButtonType(
                                                            editingButton = currentDraft,
                                                            savedButton = currentSubPage.button,
                                                        ),
                                                    )
                                                },
                                                onOpenKeyboardPicker = { currentDraft ->
                                                    pushSubPageFromEdit(
                                                        currentDraft,
                                                        MacroPadSubPage.ChooseKeyboardKey(
                                                            button = currentSubPage.button,
                                                            draftButton = currentDraft,
                                                        ),
                                                    )
                                                },
                                                onOpenGamepadPicker = { currentDraft, slotIndex ->
                                                    pushSubPageFromEdit(
                                                        currentDraft,
                                                        MacroPadSubPage.ChooseGamepadButton(
                                                            button = currentSubPage.button,
                                                            draftButton = currentDraft,
                                                            slotIndex = slotIndex,
                                                        ),
                                                    )
                                                },
                                                onOpenMousePicker = { currentDraft ->
                                                    pushSubPageFromEdit(
                                                        currentDraft,
                                                        MacroPadSubPage.ChooseMouseAction(
                                                            button = currentSubPage.button,
                                                            draftButton = currentDraft,
                                                        ),
                                                    )
                                                },
                                                onOpenMirrorPicker = { currentDraft ->
                                                    pushSubPageFromEdit(
                                                        currentDraft,
                                                        MacroPadSubPage.ChooseMirrorAction(
                                                            button = currentSubPage.button,
                                                            draftButton = currentDraft,
                                                        ),
                                                    )
                                                },
                                                onOpenOverlayPicker = { currentDraft ->
                                                    pushSubPageFromEdit(
                                                        currentDraft,
                                                        MacroPadSubPage.ChooseOverlayAction(
                                                            button = currentSubPage.button,
                                                            draftButton = currentDraft,
                                                        ),
                                                    )
                                                },
                                                onOpenLayoutPicker = { currentDraft ->
                                                    pushSubPageFromEdit(
                                                        currentDraft,
                                                        MacroPadSubPage.ChooseLayoutAction(
                                                            button = currentSubPage.button,
                                                            draftButton = currentDraft,
                                                        ),
                                                    )
                                                },
                                                onOpenMacroPicker = { currentDraft ->
                                                    pushSubPageFromEdit(
                                                        currentDraft,
                                                        MacroPadSubPage.ChooseMacroAction(
                                                            button = currentSubPage.button,
                                                            draftButton = currentDraft,
                                                        ),
                                                    )
                                                },
                                                onDuplicate = { btn ->
                                                    buttonDraft = null
                                                    activeLayout?.id?.let { MacroPadState.duplicateButtonInLayout(btn, it) }
                                                    MacroPadNavState.pop()
                                                },
                                                onCopyToLayout = { btn ->
                                                    MacroPadNavState.push(MacroPadSubPage.CopyButton(btn))
                                                },
                                                onDelete = { btn ->
                                                    buttonDraft = null
                                                    activeLayout?.let { lay ->
                                                        MacroPadState.updateLayout(
                                                            lay.copy(buttons = lay.buttons.filter { it.id != btn.id }),
                                                        )
                                                    }
                                                    DialogToastManager.show(
                                                        context.getString(R.string.macropad_button_deleted_toast),
                                                    )
                                                    MacroPadNavState.pop()
                                                },
                                                onDiscard = {
                                                    buttonDraft = null
                                                    MacroPadNavState.pop()
                                                },
                                                onSave = { savedBtn ->
                                                    buttonDraft = null
                                                    val currentProfile = MacroPadState.activeProfile.value
                                                    val lay =
                                                        currentProfile?.layouts?.firstOrNull { it.id == currentProfile.activeLayoutId }
                                                            ?: currentProfile?.layouts?.firstOrNull()
                                                            ?: activeLayout
                                                    if (lay != null) {
                                                        val isExisting = lay.buttons.any { it.id == savedBtn.id }
                                                        val updatedButtons =
                                                            if (isExisting) {
                                                                lay.buttons.map { if (it.id == savedBtn.id) savedBtn else it }
                                                            } else {
                                                                lay.buttons + savedBtn
                                                            }
                                                        MacroPadState.updateLayout(lay.copy(buttons = updatedButtons))
                                                        MacroPadState.clearPreviewLayout()
                                                    }
                                                    MacroPadNavState.pop()
                                                },
                                            )
                                        }
                                    }

                                     is MacroPadSubPage.ButtonCustomImage -> {
                                         val effectiveButton =
                                             buttonDraft?.takeIf { it.id == (currentSubPage.button?.id ?: currentSubPage.draftButton.id) }
                                                 ?: currentSubPage.draftButton
                                         GamepadDeck(
                                             breadcrumbs =
                                                 listOf(
                                                     stringResource(R.string.macropad_editor_section_buttons),
                                                     effectiveButton.label.ifBlank {
                                                         stringResource(
                                                             if (currentSubPage.button != null) {
                                                                 R.string.macropad_editor_section_button_settings
                                                             } else {
                                                                 R.string.macropad_editor_add_button
                                                             },
                                                         )
                                                     },
                                                     stringResource(R.string.button_settings_custom_image),
                                                 ),
                                         ) {
                                             ButtonCustomImageSubPageContent(
                                                 button = currentSubPage.button,
                                                 draftButton = effectiveButton,
                                                 activeLayout = activeLayout,
                                                 accentColor = globalAccentColor,
                                                 onOpenPicker = { inFlightDraft ->
                                                     buttonDraft = inFlightDraft
                                                     val isGrid = activeLayout?.isGridMode == true
                                                     val aspect =
                                                         if (isGrid) {
                                                             GridLayoutMath.cellAspectRatio(
                                                                 cols = activeLayout?.effectiveGridCols ?: 1,
                                                                 rows = activeLayout?.effectiveGridRows ?: 1,
                                                                 colSpan = inFlightDraft.colSpan,
                                                                 rowSpan = inFlightDraft.rowSpan,
                                                             )
                                                         } else {
                                                             (inFlightDraft.buttonSize.cols.toFloat() / inFlightDraft.buttonSize.rows.toFloat().coerceAtLeast(1f)).coerceAtLeast(0.1f)
                                                         }
                                                     MacroPadNavState.push(
                                                         MacroPadSubPage.ChooseButtonImage(
                                                             button = currentSubPage.button,
                                                             draftButton = inFlightDraft,
                                                             aspectRatio = aspect,
                                                             isGridMode = isGrid,
                                                         ),
                                                     )
                                                 },
                                                 onOpenScrape = { inFlightDraft ->
                                                     buttonDraft = inFlightDraft
                                                     val isGrid = activeLayout?.isGridMode == true
                                                     val aspect =
                                                         if (isGrid) {
                                                             GridLayoutMath.cellAspectRatio(
                                                                 cols = activeLayout?.effectiveGridCols ?: 1,
                                                                 rows = activeLayout?.effectiveGridRows ?: 1,
                                                                 colSpan = inFlightDraft.colSpan,
                                                                 rowSpan = inFlightDraft.rowSpan,
                                                             )
                                                         } else {
                                                             (inFlightDraft.buttonSize.cols.toFloat() / inFlightDraft.buttonSize.rows.toFloat().coerceAtLeast(1f)).coerceAtLeast(0.1f)
                                                         }
                                                     MacroPadNavState.push(
                                                         MacroPadSubPage.SteamGridDbScrape(
                                                             layoutId = activeLayout?.id ?: "",
                                                             targetButton = currentSubPage.button,
                                                             draftButton = inFlightDraft,
                                                             aspectRatio = aspect,
                                                             isGridMode = isGrid,
                                                         ),
                                                     )
                                                 },
                                                 onDiscard = {
                                                     MacroPadNavState.pop()
                                                 },
                                                 onConfirm = { updatedDraft ->
                                                     buttonDraft = updatedDraft
                                                     val targetStack =
                                                         subPageStack.dropLast(1).map { subPage ->
                                                             if (subPage is MacroPadSubPage.EditButton) {
                                                                 subPage.copy(draftButton = updatedDraft)
                                                             } else {
                                                                 subPage
                                                             }
                                                         }
                                                     MacroPadNavState.setStack(targetStack)
                                                 },
                                             )
                                         }
                                     }

                                     is MacroPadSubPage.ButtonColor -> {
                                        val effectiveButton =
                                            buttonDraft?.takeIf { it.id == (currentSubPage.button?.id ?: currentSubPage.draftButton.id) }
                                                ?: currentSubPage.draftButton
                                        GamepadDeck(
                                            breadcrumbs =
                                                listOf(
                                                    stringResource(R.string.macropad_editor_section_buttons),
                                                    effectiveButton.label.ifBlank {
                                                        stringResource(
                                                            if (currentSubPage.button != null) {
                                                                R.string.macropad_editor_section_button_settings
                                                            } else {
                                                                R.string.macropad_editor_add_button
                                                            },
                                                        )
                                                    },
                                                    stringResource(currentSubPage.target.titleResId),
                                                ),
                                        ) {
                                            ButtonColorSubPageContent(
                                                button = effectiveButton,
                                                savedButton = currentSubPage.button,
                                                activeLayout = activeLayout,
                                                target = currentSubPage.target,
                                                accentColor = globalAccentColor,
                                                onColorOptionChanged = { option ->
                                                    val updatedDraft = effectiveButton.withColorOption(currentSubPage.target, option)
                                                    buttonDraft = updatedDraft
                                                    MacroPadState.setPreviewButton(updatedDraft)
                                                },
                                                onOpenColorWheel = { title, breadcrumbs, initialColor, inFlightButton ->
                                                    buttonDraft = inFlightButton
                                                    MacroPadNavState.push(
                                                        MacroPadSubPage.ColorWheel(
                                                            title = title,
                                                            breadcrumbs = breadcrumbs,
                                                            initialColor = initialColor,
                                                            section = EditorSection.BUTTONS,
                                                            onColorChange = { liveColor ->
                                                                val option = ColorOption.Custom(liveColor.toArgb())
                                                                val base = buttonDraft ?: inFlightButton
                                                                val liveButton = base.withColorOption(currentSubPage.target, option)
                                                                buttonDraft = liveButton
                                                                MacroPadState.setPreviewButton(liveButton)
                                                            },
                                                        ),
                                                    )
                                                },
                                            )
                                        }
                                    }

                                    is MacroPadSubPage.ChooseKeyboardKey -> {
                                        val effectiveButton = currentSubPage.draftButton
                                        val currentKeyAction = effectiveButton.action as? PadAction.KeyboardKey
                                        val currentKeycode = currentKeyAction?.keycode ?: LinuxKeycodes.KEY_SPACE
                                        GamepadDeck(
                                            breadcrumbs =
                                                listOf(
                                                    stringResource(R.string.macropad_editor_section_buttons),
                                                    effectiveButton.label.ifBlank {
                                                        stringResource(
                                                            if (currentSubPage.button != null) {
                                                                R.string.macropad_editor_section_button_settings
                                                            } else {
                                                                R.string.macropad_editor_add_button
                                                            },
                                                        )
                                                    },
                                                    stringResource(R.string.macropad_picker_visual_keyboard_title),
                                                ),
                                        ) {
                                            VisualKeyboardPicker(
                                                selectedKeycode = currentKeycode,
                                                accentColor = colors.accent,
                                                onSelectKey = { keycode, label ->
                                                    val newAction =
                                                        PadAction.KeyboardKey(
                                                            keycode = keycode,
                                                            label = label,
                                                            modifiers = currentKeyAction?.modifiers ?: emptyList(),
                                                        )
                                                    val updatedDraft = effectiveButton.copy(action = newAction)
                                                    buttonDraft = updatedDraft
                                                    MacroPadNavState.setStack(
                                                        subPageStack.dropLast(1).map { subPage ->
                                                            if (subPage is MacroPadSubPage.EditButton) {
                                                                subPage.copy(draftButton = updatedDraft)
                                                            } else {
                                                                subPage
                                                            }
                                                        },
                                                    )
                                                },
                                            )
                                        }
                                    }

                                    is MacroPadSubPage.ChooseGamepadButton -> {
                                        val effectiveButton = currentSubPage.draftButton
                                        val currentBtnAction = effectiveButton.action as? PadAction.GamepadButton
                                        val slotIndex = currentSubPage.slotIndex
                                        val currentBtnCode =
                                            when (slotIndex) {
                                                1 -> currentBtnAction?.extraBtnCodes?.getOrNull(0) ?: -1
                                                2 -> currentBtnAction?.extraBtnCodes?.getOrNull(1) ?: -1
                                                3 -> currentBtnAction?.extraBtnCodes?.getOrNull(2) ?: -1
                                                else -> currentBtnAction?.btnCode ?: GamepadKeycodes.BTN_SOUTH
                                            }
                                        val swapFaceButtons by MacroPadSettings.gamepadSwapFaceButtons.collectAsStateWithLifecycle()
                                        val slotTitle =
                                            when (slotIndex) {
                                                1 -> stringResource(R.string.macropad_picker_label_extra_1)
                                                2 -> stringResource(R.string.macropad_picker_label_extra_2)
                                                3 -> stringResource(R.string.macropad_picker_label_extra_3)
                                                else -> stringResource(R.string.macropad_picker_visual_gamepad_title)
                                            }
                                        GamepadDeck(
                                            breadcrumbs =
                                                listOf(
                                                    stringResource(R.string.macropad_editor_section_buttons),
                                                    effectiveButton.label.ifBlank {
                                                        stringResource(
                                                            if (currentSubPage.button != null) {
                                                                R.string.macropad_editor_section_button_settings
                                                            } else {
                                                                R.string.macropad_editor_add_button
                                                            },
                                                        )
                                                    },
                                                    slotTitle,
                                                ),
                                        ) {
                                            VisualGamepadPicker(
                                                selectedBtnCode = currentBtnCode,
                                                accentColor = colors.accent,
                                                onSelectButton = { preset ->
                                                    val baseAction =
                                                        currentBtnAction
                                                            ?: PadAction.GamepadButton(GamepadKeycodes.BTN_SOUTH, "A")
                                                    val newAction =
                                                        updateGamepadButtonSlot(
                                                            currentAction = baseAction,
                                                            slotIndex = slotIndex,
                                                            selectedCode = preset.code,
                                                            swapFaceButtons = swapFaceButtons,
                                                        )
                                                    val updatedDraft = effectiveButton.copy(action = newAction)
                                                    buttonDraft = updatedDraft
                                                    MacroPadNavState.setStack(
                                                        subPageStack.dropLast(1).map { subPage ->
                                                            if (subPage is MacroPadSubPage.EditButton) {
                                                                subPage.copy(draftButton = updatedDraft)
                                                            } else {
                                                                subPage
                                                            }
                                                        },
                                                    )
                                                },
                                                onClear =
                                                    if (slotIndex in 1..3 && currentBtnCode != -1) {
                                                        {
                                                            val baseAction =
                                                                currentBtnAction
                                                                    ?: PadAction.GamepadButton(GamepadKeycodes.BTN_SOUTH, "A")
                                                            val newAction =
                                                                updateGamepadButtonSlot(
                                                                    currentAction = baseAction,
                                                                    slotIndex = slotIndex,
                                                                    selectedCode = null,
                                                                    swapFaceButtons = swapFaceButtons,
                                                                )
                                                            val updatedDraft = effectiveButton.copy(action = newAction)
                                                            buttonDraft = updatedDraft
                                                            MacroPadNavState.setStack(
                                                                subPageStack.dropLast(1).map { subPage ->
                                                                    if (subPage is MacroPadSubPage.EditButton) {
                                                                        subPage.copy(draftButton = updatedDraft)
                                                                    } else {
                                                                        subPage
                                                                    }
                                                                },
                                                            )
                                                        }
                                                    } else {
                                                        null
                                                    },
                                                modifier = Modifier.firstDeckItem(),
                                            )
                                        }
                                    }

                                    is MacroPadSubPage.ChooseMouseAction -> {
                                        val effectiveButton = currentSubPage.draftButton
                                        GamepadDeck(
                                            breadcrumbs =
                                                listOf(
                                                    stringResource(R.string.macropad_editor_section_buttons),
                                                    effectiveButton.label.ifBlank {
                                                        stringResource(
                                                            if (currentSubPage.button != null) {
                                                                R.string.macropad_editor_section_button_settings
                                                            } else {
                                                                R.string.macropad_editor_add_button
                                                            },
                                                        )
                                                    },
                                                    stringResource(R.string.macropad_picker_visual_mouse_title),
                                                ),
                                        ) {
                                            VisualMousePicker(
                                                currentAction = effectiveButton.action,
                                                accentColor = colors.accent,
                                                onSelectAction = { act ->
                                                    val updatedDraft = applyActionToDraftButton(effectiveButton, act)
                                                    buttonDraft = updatedDraft
                                                    MacroPadNavState.setStack(
                                                        subPageStack.dropLast(1).map { subPage ->
                                                            if (subPage is MacroPadSubPage.EditButton) {
                                                                subPage.copy(draftButton = updatedDraft)
                                                            } else {
                                                                subPage
                                                            }
                                                        },
                                                    )
                                                },
                                            )
                                        }
                                    }

                                    is MacroPadSubPage.ChooseMirrorAction -> {
                                        val effectiveButton = currentSubPage.draftButton
                                        GamepadDeck(
                                            breadcrumbs =
                                                listOf(
                                                    stringResource(R.string.macropad_editor_section_buttons),
                                                    effectiveButton.label.ifBlank {
                                                        stringResource(
                                                            if (currentSubPage.button != null) {
                                                                R.string.macropad_editor_section_button_settings
                                                            } else {
                                                                R.string.macropad_editor_add_button
                                                            },
                                                        )
                                                    },
                                                    stringResource(R.string.macropad_picker_mirror_title),
                                                ),
                                        ) {
                                            MirrorActionPickerSubPageContent(
                                                currentAction = effectiveButton.action,
                                                accentColor = colors.accent,
                                                onSelectAction = { act ->
                                                    val updatedDraft = applyActionToDraftButton(effectiveButton, act)
                                                    buttonDraft = updatedDraft
                                                    MacroPadNavState.setStack(
                                                        subPageStack.dropLast(1).map { subPage ->
                                                            if (subPage is MacroPadSubPage.EditButton) {
                                                                subPage.copy(draftButton = updatedDraft)
                                                            } else {
                                                                subPage
                                                            }
                                                        },
                                                    )
                                                },
                                            )
                                        }
                                    }

                                    is MacroPadSubPage.ChooseOverlayAction -> {
                                        val effectiveButton = currentSubPage.draftButton
                                        GamepadDeck(
                                            breadcrumbs =
                                                listOf(
                                                    stringResource(R.string.macropad_editor_section_buttons),
                                                    effectiveButton.label.ifBlank {
                                                        stringResource(
                                                            if (currentSubPage.button != null) {
                                                                R.string.macropad_editor_section_button_settings
                                                            } else {
                                                                R.string.macropad_editor_add_button
                                                            },
                                                        )
                                                    },
                                                    stringResource(R.string.macropad_picker_overlay_title),
                                                ),
                                        ) {
                                            OverlayActionPickerSubPageContent(
                                                currentAction = effectiveButton.action,
                                                accentColor = colors.accent,
                                                onSelectAction = { act ->
                                                    val updatedDraft = applyActionToDraftButton(effectiveButton, act)
                                                    buttonDraft = updatedDraft
                                                    MacroPadNavState.setStack(
                                                        subPageStack.dropLast(1).map { subPage ->
                                                            if (subPage is MacroPadSubPage.EditButton) {
                                                                subPage.copy(draftButton = updatedDraft)
                                                            } else {
                                                                subPage
                                                            }
                                                        },
                                                    )
                                                },
                                            )
                                        }
                                    }

                                    is MacroPadSubPage.ChooseLayoutAction -> {
                                        val effectiveButton = currentSubPage.draftButton
                                        GamepadDeck(
                                            breadcrumbs =
                                                listOf(
                                                    stringResource(R.string.macropad_editor_section_buttons),
                                                    effectiveButton.label.ifBlank {
                                                        stringResource(
                                                            if (currentSubPage.button != null) {
                                                                R.string.macropad_editor_section_button_settings
                                                            } else {
                                                                R.string.macropad_editor_add_button
                                                            },
                                                        )
                                                    },
                                                    stringResource(R.string.macropad_picker_layout_title),
                                                ),
                                        ) {
                                            LayoutActionPickerSubPageContent(
                                                currentAction = effectiveButton.action,
                                                accentColor = colors.accent,
                                                onSelectAction = { act ->
                                                    val updatedDraft = applyActionToDraftButton(effectiveButton, act)
                                                    buttonDraft = updatedDraft
                                                    MacroPadNavState.setStack(
                                                        subPageStack.dropLast(1).map { subPage ->
                                                            if (subPage is MacroPadSubPage.EditButton) {
                                                                subPage.copy(draftButton = updatedDraft)
                                                            } else {
                                                                subPage
                                                            }
                                                        },
                                                    )
                                                },
                                            )
                                        }
                                    }

                                    is MacroPadSubPage.ChooseIcon -> {
                                        val parentDraftButton =
                                            buttonDraft ?: subPageStack.filterIsInstance<MacroPadSubPage.EditButton>().lastOrNull()?.let {
                                                it.draftButton ?: it.button
                                            }
                                        val currentDraft = buttonDraft ?: parentDraftButton
                                        val layoutTextOpt = activeLayout?.buttonTextColor ?: ColorOption.Neutral
                                        val effectiveTextOpt = currentDraft?.buttonTextColor ?: layoutTextOpt
                                        val effectiveIconColor = resolveColorOption(effectiveTextOpt, globalAccentColor, globalAccentColor)
                                        GamepadDeck(
                                            breadcrumbs =
                                                listOf(
                                                    stringResource(R.string.macropad_editor_section_buttons),
                                                    stringResource(R.string.macropad_icon_picker_title),
                                                ),
                                        ) {
                                            ChooseIconSubPageContent(
                                                selectedIcon = currentDraft?.iconName,
                                                accentColor = effectiveIconColor,
                                                filled = currentDraft?.iconFilled ?: true,
                                                onFilledChange = { filled ->
                                                    val cur = buttonDraft ?: parentDraftButton
                                                    val updated = cur?.copy(iconFilled = filled)
                                                    buttonDraft = updated
                                                    if (updated != null) {
                                                        MacroPadState.setPreviewButton(updated)
                                                    }
                                                },
                                                onSelect = { icon ->
                                                    val cur = buttonDraft ?: parentDraftButton
                                                    val updated = cur?.copy(iconName = icon, imageAssetId = null)
                                                    buttonDraft = updated
                                                    if (updated != null) {
                                                        MacroPadState.setPreviewButton(updated)
                                                    }
                                                    val targetStack =
                                                        subPageStack.dropLast(1).map { subPage ->
                                                             if (subPage is MacroPadSubPage.EditButton) {
                                                                 subPage.copy(draftButton = updated)
                                                             } else {
                                                                 subPage
                                                             }
                                                        }
                                                    MacroPadNavState.setStack(targetStack)
                                                },
                                            )
                                        }
                                    }

                                    is MacroPadSubPage.ChooseButtonImage -> {
                                        val parentDraftButton =
                                            buttonDraft ?: subPageStack.filterIsInstance<MacroPadSubPage.EditButton>().lastOrNull()?.let {
                                                it.draftButton ?: it.button
                                            } ?: currentSubPage.draftButton
                                        GamepadDeck(
                                            breadcrumbs =
                                                listOf(
                                                    stringResource(R.string.macropad_editor_section_buttons),
                                                    stringResource(R.string.button_image_picker_title),
                                                ),
                                        ) {
                                            LocalImagePickerSubPageContent(
                                                onSelectImage = { uri ->
                                                    scope.launch {
                                                        val decoded =
                                                            withContext(Dispatchers.IO) {
                                                                BitmapUtils.decodeScaledBitmapFromUri(
                                                                    context,
                                                                    uri,
                                                                    targetW = BTN_IMAGE_DECODE_PX,
                                                                    targetH = BTN_IMAGE_DECODE_PX,
                                                                )
                                                            }
                                                        if (decoded != null) {
                                                            val savedAssetId = PadIconStore.put(context, decoded)
                                                            if (savedAssetId != null) {
                                                                val updated =
                                                                    parentDraftButton.copy(
                                                                        imageAssetId = savedAssetId,
                                                                        iconName = null,
                                                                        imageScaleMode = BackgroundScaleMode.FILL,
                                                                        imageScale = 1.0f,
                                                                        imageOffsetX = 0f,
                                                                        imageOffsetY = 0f,
                                                                    )
                                                                buttonDraft = updated
                                                                MacroPadState.setPreviewButton(updated)
                                                                val targetStack =
                                                                    subPageStack.filterNot {
                                                                        it is MacroPadSubPage.ChooseButtonImage || it is MacroPadSubPage.ChooseIcon || it is MacroPadSubPage.CropButtonImage
                                                                    }.map { subPage ->
                                                                        if (subPage is MacroPadSubPage.EditButton) {
                                                                            subPage.copy(draftButton = updated)
                                                                        } else if (subPage is MacroPadSubPage.ButtonCustomImage) {
                                                                            subPage.copy(draftButton = updated)
                                                                        } else {
                                                                            subPage
                                                                        }
                                                                    }
                                                                MacroPadNavState.setStack(targetStack)
                                                            }
                                                        }
                                                    }
                                                },
                                                onOpenSteamGridDb = {
                                                    val layId = activeLayout?.id ?: ""
                                                    MacroPadNavState.push(
                                                        MacroPadSubPage.SteamGridDbScrape(
                                                            layoutId = layId,
                                                            targetButton = currentSubPage.button,
                                                            draftButton = parentDraftButton,
                                                            aspectRatio = currentSubPage.aspectRatio,
                                                            isGridMode = currentSubPage.isGridMode || activeLayout?.isGridMode == true,
                                                        ),
                                                    )
                                                },
                                                onOpenSystemPicker = {
                                                    buttonDraft = parentDraftButton
                                                    ButtonImagePickerManager.requestImagePicker()
                                                },
                                            )
                                        }
                                    }

                                    is MacroPadSubPage.CropButtonImage -> {
                                        val parentDraftButton =
                                            buttonDraft ?: subPageStack.filterIsInstance<MacroPadSubPage.EditButton>().lastOrNull()?.let {
                                                it.draftButton ?: it.button
                                            } ?: currentSubPage.draftButton

                                        DisposableEffect(currentSubPage) {
                                            MacroPadState.setCroppingButtonState(
                                                CroppingButtonState(
                                                    button = currentSubPage.button,
                                                    draftButton = parentDraftButton,
                                                    sourceBitmap = currentSubPage.bitmap.asAndroidBitmap(),
                                                    aspectRatio = currentSubPage.aspectRatio,
                                                    isGridMode = currentSubPage.isGridMode || activeLayout?.isGridMode == true,
                                                    scaleMode = currentSubPage.initialScaleMode,
                                                    scale = currentSubPage.initialScale,
                                                    offsetX = currentSubPage.initialOffsetX,
                                                    offsetY = currentSubPage.initialOffsetY,
                                                ),
                                            )
                                            onDispose {
                                                MacroPadState.setCroppingButtonState(null)
                                            }
                                        }

                                        val croppingState by MacroPadState.croppingButtonState.collectAsStateWithLifecycle()
                                        val activeCropState = croppingState

                                        GamepadDeck(
                                            breadcrumbs =
                                                listOf(
                                                    stringResource(R.string.macropad_editor_section_buttons),
                                                    stringResource(R.string.layout_settings_crop_image_title),
                                                ),
                                        ) {
                                            ImageCropSubPageContent(
                                                bitmap = currentSubPage.bitmap,
                                                aspectRatio = currentSubPage.aspectRatio,
                                                scaleMode = activeCropState?.scaleMode ?: BackgroundScaleMode.FILL,
                                                scale = activeCropState?.scale ?: 1.0f,
                                                offsetX = activeCropState?.offsetX ?: 0f,
                                                offsetY = activeCropState?.offsetY ?: 0f,
                                                onScaleModeChange = { mode ->
                                                    MacroPadState.updateCroppingButtonScaleMode(mode)
                                                },
                                                onTransformChange = { s, x, y ->
                                                    MacroPadState.updateCroppingButtonTransform(s, x, y)
                                                },
                                                onCancel = {
                                                    MacroPadNavState.pop()
                                                },
                                                onConfirm = { _ ->
                                                    scope.launch {
                                                        val assetId =
                                                            parentDraftButton.imageAssetId
                                                                ?: PadIconStore.put(context, currentSubPage.bitmap.asAndroidBitmap())
                                                        if (assetId != null) {
                                                            val updated =
                                                                parentDraftButton.copy(
                                                                    imageAssetId = assetId,
                                                                    iconName = null,
                                                                    imageScaleMode = activeCropState?.scaleMode ?: BackgroundScaleMode.FILL,
                                                                    imageScale = activeCropState?.scale ?: 1.0f,
                                                                    imageOffsetX = activeCropState?.offsetX ?: 0f,
                                                                    imageOffsetY = activeCropState?.offsetY ?: 0f,
                                                                )
                                                            buttonDraft = updated
                                                            MacroPadState.setPreviewButton(updated)
                                                            val targetStack =
                                                                subPageStack.filterNot {
                                                                    it is MacroPadSubPage.CropButtonImage || it is MacroPadSubPage.ChooseButtonImage || it is MacroPadSubPage.ChooseIcon
                                                                }.map { subPage ->
                                                                    if (subPage is MacroPadSubPage.EditButton) {
                                                                        subPage.copy(draftButton = updated)
                                                                    } else {
                                                                        subPage
                                                                    }
                                                                }
                                                            MacroPadNavState.setStack(targetStack)
                                                        } else {
                                                            MacroPadNavState.pop()
                                                        }
                                                    }
                                                },
                                            )
                                        }
                                    }

                                    is MacroPadSubPage.CopyButton -> {
                                        GamepadDeck(
                                            breadcrumbs =
                                                listOf(
                                                    stringResource(R.string.macropad_editor_section_buttons),
                                                    stringResource(R.string.macropad_editor_copy_layout_select),
                                                ),
                                        ) {
                                            CopyButtonSubPageContent(
                                                title = stringResource(R.string.macropad_editor_copy_layout_select),
                                                profiles = profiles,
                                                excludeLayoutId = activeLayout?.id,
                                                accentColor = colors.accent,
                                                onSelect = { targetProfileId, targetLayoutId ->
                                                    MacroPadState.copyButtonToLayout(
                                                        currentSubPage.button,
                                                        profile.id,
                                                        targetProfileId,
                                                        targetLayoutId,
                                                    )
                                                    MacroPadNavState.pop()
                                                },
                                            )
                                        }
                                    }

                                    is MacroPadSubPage.ChooseMacroAction -> {
                                        val effectiveButton = currentSubPage.draftButton
                                        GamepadDeck(
                                            breadcrumbs =
                                                listOf(
                                                    stringResource(R.string.macropad_editor_section_buttons),
                                                    effectiveButton.label.ifBlank {
                                                        stringResource(
                                                            if (currentSubPage.button != null) {
                                                                R.string.macropad_editor_section_button_settings
                                                            } else {
                                                                R.string.macropad_editor_add_button
                                                            },
                                                        )
                                                    },
                                                    stringResource(R.string.macropad_action_macro),
                                                ),
                                        ) {
                                            MacroActionPickerSubPageContent(
                                                currentAction = effectiveButton.action,
                                                accentColor = colors.accent,
                                                onSelectAction = { act ->
                                                    val updatedDraft = applyActionToDraftButton(effectiveButton, act)
                                                    MacroPadNavState.setStack(
                                                        subPageStack.dropLast(1).map { subPage ->
                                                            if (subPage is MacroPadSubPage.EditButton) {
                                                                subPage.copy(draftButton = updatedDraft)
                                                            } else {
                                                                subPage
                                                            }
                                                        },
                                                    )
                                                },
                                            )
                                        }
                                    }

                                    is MacroPadSubPage.ChooseMacroMode -> {
                                        val privdState by PrivdManager.state.collectAsStateWithLifecycle()
                                        val defaultMacroName = stringResource(R.string.macropad_macro_default_name)
                                        val existingMacroNames = profile.macros.map { it.name }

                                        fun handleCreateMacro(action: (Macro) -> Unit = {}) {
                                            if (privdState != PrivdState.RUNNING) {
                                                DialogToastManager.show(context.getString(R.string.privd_error_daemon_unreachable))
                                                return
                                            }
                                            val newMacro =
                                                Macro(
                                                    id = UUID.randomUUID().toString(),
                                                    name = existingMacroNames.nextUniqueName(defaultMacroName),
                                                    steps = emptyList(),
                                                )
                                            MacroPadNavState.setStack(
                                                subPageStack.dropLast(1) +
                                                    MacroPadSubPage.MacroTimeline(macro = null, draftMacro = newMacro),
                                            )
                                            action(newMacro)
                                        }

                                        val breadcrumbs =
                                            listOf(
                                                stringResource(R.string.macropad_editor_manage_macros),
                                                stringResource(R.string.macropad_macro_create_title),
                                            )

                                        GamepadDeck(breadcrumbs = breadcrumbs) {
                                            ChooseMacroModeSubPageContent(
                                                onRecordGamepad = {
                                                    handleCreateMacro {
                                                        AppStateManager.suspendCurrentAndDismiss()
                                                        PhysicalGamepadRecordingManager.startRecording()
                                                    }
                                                },
                                                onBuildManual = { handleCreateMacro() },
                                                onRecordTouchTap = {
                                                    handleCreateMacro {
                                                        AppStateManager.suspendCurrentAndDismiss()
                                                        TouchRecordingManager.requestRecording(TouchRecordingMode.TAP)
                                                    }
                                                },
                                                onRecordTouchGesture = {
                                                    handleCreateMacro {
                                                        AppStateManager.suspendCurrentAndDismiss()
                                                        TouchRecordingManager.requestRecording(TouchRecordingMode.GESTURE)
                                                    }
                                                },
                                            )
                                        }
                                    }

                                    is MacroPadSubPage.MacroTimeline -> {
                                        val macro =
                                            currentSubPage.effectiveMacro
                                                ?: profile?.macros?.firstOrNull { it.id == currentSubPage.macroId }
                                                ?: profiles.flatMap { it.macros }.firstOrNull { it.id == currentSubPage.macroId }
                                        val savedMacro = currentSubPage.macro
                                        if (macro != null) {
                                            GamepadDeck(
                                                breadcrumbs =
                                                    listOf(
                                                        stringResource(R.string.macropad_editor_manage_macros),
                                                        macro.name.ifBlank { stringResource(R.string.macropad_editor_open_timeline_title) },
                                                    ),
                                            ) {
                                                MacroTimelineSubPageContent(
                                                    macro = macro,
                                                    savedMacro = savedMacro,
                                                    accentColor = colors.accent,
                                                    onOpenManualSteps = { draftMacro ->
                                                        val updatedStack =
                                                            subPageStack.map { page ->
                                                                if (page is MacroPadSubPage.MacroTimeline &&
                                                                    page.macroId == draftMacro.id
                                                                ) {
                                                                    page.copy(draftMacro = draftMacro)
                                                                } else {
                                                                    page
                                                                }
                                                            } +
                                                                MacroPadSubPage.ManualMacroSteps(
                                                                    macro = currentSubPage.macro,
                                                                    draftMacro = draftMacro,
                                                                )
                                                        MacroPadNavState.setStack(updatedStack)
                                                    },
                                                    onDiscard = {
                                                        MacroPadNavState.pop()
                                                    },
                                                    onSave = { updatedMacro ->
                                                        if (savedMacro == null) {
                                                            MacroPadState.addMacro(updatedMacro)
                                                        } else {
                                                            MacroPadState.updateMacro(updatedMacro)
                                                        }
                                                        MacroPadNavState.pop()
                                                    },
                                                    onDelete = {
                                                        val deletedName = macro.name
                                                        if (savedMacro != null) {
                                                            MacroPadState.deleteMacro(macro.id)
                                                        }
                                                        MacroPadNavState.pop()
                                                        DialogToastManager.show(
                                                            context.getString(R.string.macropad_macro_deleted_toast, deletedName),
                                                        )
                                                    },
                                                )
                                            }
                                        }
                                    }

                                    is MacroPadSubPage.ManualMacroSteps -> {
                                        val macro =
                                            currentSubPage.effectiveMacro
                                                ?: profile?.macros?.firstOrNull { it.id == currentSubPage.macroId }
                                                ?: profiles.flatMap { it.macros }.firstOrNull { it.id == currentSubPage.macroId }
                                        if (macro != null) {
                                            GamepadDeck(
                                                breadcrumbs =
                                                    listOf(
                                                        stringResource(R.string.macropad_editor_manage_macros),
                                                        macro.name.ifBlank { stringResource(R.string.macropad_editor_open_timeline_title) },
                                                        stringResource(R.string.macropad_macro_manual_steps_title),
                                                    ),
                                            ) {
                                                ManualMacroStepsSubPageContent(
                                                    macro = macro,
                                                    accentColor = colors.accent,
                                                    onOpenAddStep = {
                                                        MacroPadNavState.setStack(
                                                            subPageStack +
                                                                MacroPadSubPage.MacroStepEdit(
                                                                    macro = currentSubPage.macro,
                                                                    draftMacro = macro,
                                                                    stepIndex = null,
                                                                ),
                                                        )
                                                    },
                                                    onOpenEditStep = { stepIdx ->
                                                        MacroPadNavState.setStack(
                                                            subPageStack +
                                                                MacroPadSubPage.MacroStepEdit(
                                                                    macro = currentSubPage.macro,
                                                                    draftMacro = macro,
                                                                    stepIndex = stepIdx,
                                                                ),
                                                        )
                                                    },
                                                    onOpenReorderSteps = {
                                                        MacroPadNavState.setStack(
                                                            subPageStack +
                                                                MacroPadSubPage.ReorderMacroSteps(
                                                                    macro = currentSubPage.macro,
                                                                    draftMacro = macro,
                                                                ),
                                                        )
                                                    },
                                                )
                                            }
                                        }
                                    }

                                    is MacroPadSubPage.MacroStepEdit -> {
                                        val macro =
                                            currentSubPage.effectiveMacro
                                                ?: profile?.macros?.firstOrNull { it.id == currentSubPage.macroId }
                                                ?: profiles.flatMap { it.macros }.firstOrNull { it.id == currentSubPage.macroId }
                                        if (macro != null &&
                                            (currentSubPage.stepIndex == null || currentSubPage.stepIndex < macro.steps.size)
                                        ) {
                                            val step = currentSubPage.stepIndex?.let { macro.steps.getOrNull(it) }
                                            GamepadDeck(
                                                breadcrumbs =
                                                    listOf(
                                                        stringResource(R.string.macropad_editor_manage_macros),
                                                        macro.name.ifBlank { stringResource(R.string.macropad_editor_open_timeline_title) },
                                                        stringResource(
                                                            if (step == null) {
                                                                R.string.macropad_macro_step_new
                                                            } else {
                                                                R.string.macropad_macro_step_edit
                                                            },
                                                        ),
                                                    ),
                                            ) {
                                                MacroStepEditSubPageContent(
                                                    macroName = macro.name,
                                                    step = step,
                                                    stepIndex = currentSubPage.stepIndex,
                                                    accentColor = colors.accent,
                                                    suggestedStartTimeMs = macro.steps.totalDurationMs(),
                                                    initialShiftMode = ShiftMode.END_DELTA,
                                                    onConfirm = { newStep, shiftMode ->
                                                        val (updatedSteps, targetIndex) =
                                                            if (currentSubPage.stepIndex != null && step != null) {
                                                                applyShiftSubsequent(
                                                                    macro.steps,
                                                                    currentSubPage.stepIndex,
                                                                    step,
                                                                    newStep,
                                                                    shiftMode,
                                                                ) to currentSubPage.stepIndex
                                                            } else {
                                                                (macro.steps + newStep) to macro.steps.size
                                                            }
                                                        val updatedMacro = macro.copy(steps = updatedSteps)
                                                        val parentDepth = subPageStack.size - 1
                                                        MacroPadNavState.recordFocusedKey(parentDepth, "macro_manual_step_$targetIndex")
                                                        val updatedStack = updateStackDraftMacro(subPageStack.dropLast(1), updatedMacro)
                                                        MacroPadNavState.setStack(updatedStack)
                                                        if (currentSubPage.macro != null) {
                                                            MacroPadState.updateMacro(updatedMacro)
                                                        }
                                                    },
                                                    onDiscard = { MacroPadNavState.pop() },
                                                    onDuplicate = { dupStep ->
                                                        val newStart = macro.steps.totalDurationMs()
                                                        val duplicated = dupStep.withStartTime(newStart)
                                                        val updatedMacro = macro.copy(steps = macro.steps + duplicated)
                                                        val parentDepth = subPageStack.size - 1
                                                        val newIndex = macro.steps.size
                                                        MacroPadNavState.recordFocusedKey(parentDepth, "macro_manual_step_$newIndex")
                                                        val updatedStack = updateStackDraftMacro(subPageStack.dropLast(1), updatedMacro)
                                                        MacroPadNavState.setStack(updatedStack)
                                                        if (currentSubPage.macro != null) {
                                                            MacroPadState.updateMacro(updatedMacro)
                                                        }
                                                        DialogToastManager.show(
                                                            context.getString(R.string.macropad_macro_step_duplicate),
                                                        )
                                                    },
                                                    onDelete = {
                                                        if (currentSubPage.stepIndex != null) {
                                                            val updatedSteps =
                                                                macro.steps.filterIndexed { i, _ ->
                                                                    i !=
                                                                        currentSubPage.stepIndex
                                                                }
                                                            val updatedMacro = macro.copy(steps = updatedSteps)
                                                            val parentDepth = subPageStack.size - 1
                                                            if (updatedSteps.isNotEmpty()) {
                                                                val targetIndex =
                                                                    if (currentSubPage.stepIndex >= updatedSteps.size) {
                                                                        updatedSteps.size - 1
                                                                    } else {
                                                                        currentSubPage.stepIndex
                                                                    }
                                                                MacroPadNavState.recordFocusedKey(
                                                                    parentDepth,
                                                                    "macro_manual_step_$targetIndex",
                                                                )
                                                            } else {
                                                                MacroPadNavState.removeFocusedKey(parentDepth)
                                                            }
                                                            val updatedStack = updateStackDraftMacro(subPageStack.dropLast(1), updatedMacro)
                                                            MacroPadNavState.setStack(updatedStack)
                                                            if (currentSubPage.macro != null) {
                                                                MacroPadState.updateMacro(updatedMacro)
                                                            }
                                                            DialogToastManager.show(
                                                                context.getString(R.string.macropad_macro_step_delete),
                                                            )
                                                        }
                                                    },
                                                )
                                            }
                                        }
                                    }

                                    is MacroPadSubPage.ReorderMacroSteps -> {
                                        val macro =
                                            currentSubPage.effectiveMacro
                                                ?: profile?.macros?.firstOrNull { it.id == currentSubPage.macroId }
                                                ?: profiles.flatMap { it.macros }.firstOrNull { it.id == currentSubPage.macroId }
                                        if (macro != null) {
                                            val swapFaceButtons by MacroPadSettings.gamepadSwapFaceButtons.collectAsStateWithLifecycle()
                                            GamepadReorderDeck(
                                                items = macro.steps,
                                                itemKey = { step -> "${step.startTimeMs}_${step.durationMs}_${step.hashCode()}" },
                                                itemTitle = { step ->
                                                    val stepIdx = macro.steps.indexOf(step)
                                                    "${stepIdx + 1}. ${stepTypeLabel(
                                                        step,
                                                        context,
                                                    )}: ${stepActionDescription(step, swapFaceButtons, context)}"
                                                },
                                                itemDescription = { step ->
                                                    context.getString(
                                                        R.string.macropad_macro_step_timing,
                                                        step.startTimeMs,
                                                        step.durationMs,
                                                    )
                                                },
                                                itemIcon = { step -> stepIcon(step) },
                                                onReorder = { reorderedSteps ->
                                                    val updatedMacro = macro.copy(steps = reorderedSteps)
                                                    val updatedStack =
                                                        subPageStack.map { page ->
                                                            when (page) {
                                                                is MacroPadSubPage.MacroTimeline -> page.copy(draftMacro = updatedMacro)
                                                                is MacroPadSubPage.ManualMacroSteps -> page.copy(draftMacro = updatedMacro)
                                                                is MacroPadSubPage.ReorderMacroSteps -> page.copy(draftMacro = updatedMacro)
                                                                else -> page
                                                            }
                                                        }
                                                    MacroPadNavState.setStack(updatedStack)
                                                    if (currentSubPage.macro != null) {
                                                        MacroPadState.updateMacro(updatedMacro)
                                                    }
                                                },
                                                breadcrumbs =
                                                    listOf(
                                                        stringResource(R.string.macropad_editor_manage_macros),
                                                        macro.name.ifBlank { stringResource(R.string.macropad_editor_open_timeline_title) },
                                                        stringResource(R.string.macropad_macro_reorder_steps_title),
                                                    ),
                                                emptyMessage = stringResource(R.string.macropad_macro_reorder_steps_empty),
                                            )
                                        }
                                    }

                                    is MacroPadSubPage.ColorWheel -> {
                                        GamepadDeck(
                                            breadcrumbs = currentSubPage.breadcrumbs,
                                        ) {
                                            ColorWheelSubPageContent(
                                                initialColor = currentSubPage.initialColor,
                                                showAlphaSlider = currentSubPage.showAlphaSlider,
                                                onColorChange = currentSubPage.onColorChange,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

// ── Decks Implementation ───────────────────────────────────────────────────

@Composable
private fun ProfilesDeck(
    profiles: List<PadProfile>,
    activeProfile: PadProfile,
    accentColor: Color,
    onSelectProfile: (String) -> Unit,
    onNewProfile: () -> Unit,
    onEditProfile: () -> Unit,
    onDuplicateProfile: () -> Unit,
    onReorderProfiles: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val firstItemFocusRequester = remember { FocusRequester() }

    GamepadChoiceCard(
        title = stringResource(R.string.quick_menu_profile_label),
        description = stringResource(R.string.macropad_editor_active_profile_desc),
        selectedText = activeProfile.name,
        icon = Icons.Rounded.Folder,
        onPrevious = { onSelectProfile(profiles.cycle(activeProfile, BumperDirection.PREV).id) },
        onNext = { onSelectProfile(profiles.cycle(activeProfile, BumperDirection.NEXT).id) },
        modifier = Modifier.firstDeckItem().focusRequester(firstItemFocusRequester),
    )

    GamepadActionCard(
        title = stringResource(R.string.macropad_editor_edit_profile_title),
        description = stringResource(R.string.macropad_editor_edit_profile_desc),
        icon = Icons.Rounded.Edit,
        onClick = onEditProfile,
    )

    GamepadActionCard(
        title = stringResource(R.string.settings_macropad_new_profile),
        description = stringResource(R.string.macropad_editor_new_profile_desc),
        icon = Icons.Rounded.Add,
        onClick = onNewProfile,
    )

    GamepadActionCard(
        title = stringResource(R.string.macropad_duplicate_profile),
        description = stringResource(R.string.macropad_editor_duplicate_profile_desc, activeProfile.name),
        icon = Icons.Rounded.ContentCopy,
        onClick = {
            onDuplicateProfile()
            scope.launch {
                try {
                    firstItemFocusRequester.requestFocus()
                } catch (_: IllegalStateException) {
                }
            }
        },
    )

    GamepadActionCard(
        title = stringResource(R.string.macropad_reorder_profiles),
        description = stringResource(R.string.macropad_editor_reorder_profiles_desc),
        icon = Icons.Rounded.SwapVert,
        onClick = onReorderProfiles,
    )
}

@Composable
private fun LayoutsDeck(
    profile: PadProfile,
    activeLayout: PadLayout?,
    accentColor: Color,
    onSelectLayout: (String) -> Unit,
    onLayoutModeChange: (PadLayoutMode) -> Unit,
    onEditLayout: () -> Unit,
    onNewLayout: () -> Unit,
    onDuplicateLayout: () -> Unit,
    onCopyLayout: () -> Unit,
    onReorderLayouts: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val firstItemFocusRequester = remember { FocusRequester() }

    val layouts = profile.layouts
    GamepadChoiceCard(
        title = stringResource(R.string.macropad_editor_section_layout),
        description = stringResource(R.string.macropad_editor_active_layout_desc, profile.name),
        selectedText = activeLayout?.name ?: stringResource(R.string.macropad_editor_none),
        icon = Icons.AutoMirrored.Rounded.ViewQuilt,
        enabled = layouts.isNotEmpty(),
        onPrevious = {
            if (layouts.isNotEmpty() &&
                activeLayout != null
            ) {
                onSelectLayout(layouts.cycle(activeLayout, BumperDirection.PREV).id)
            }
        },
        onNext = { if (layouts.isNotEmpty() && activeLayout != null) onSelectLayout(layouts.cycle(activeLayout, BumperDirection.NEXT).id) },
        modifier = Modifier.firstDeckItem().focusRequester(firstItemFocusRequester),
    )

    if (activeLayout != null) {
        val modeEntries = PadLayoutMode.entries
        val modeLabels =
            listOf(
                stringResource(R.string.layout_settings_mode_free),
                stringResource(R.string.layout_settings_mode_grid),
            )
        val modeIdx = modeEntries.indexOf(activeLayout.layoutMode).coerceAtLeast(0)

        GamepadChoiceCard(
            title = stringResource(R.string.layout_settings_mode_title),
            description =
                if (activeLayout.isGridMode) {
                    stringResource(R.string.layout_settings_mode_grid_desc)
                } else {
                    stringResource(R.string.layout_settings_mode_free_desc)
                },
            selectedText = modeLabels[modeIdx],
            icon = if (activeLayout.isGridMode) Icons.Rounded.GridView else Icons.Rounded.DashboardCustomize,
            onPrevious = {
                val nextIdx = (modeIdx - 1 + modeEntries.size) % modeEntries.size
                onLayoutModeChange(modeEntries[nextIdx])
            },
            onNext = {
                val nextIdx = (modeIdx + 1) % modeEntries.size
                onLayoutModeChange(modeEntries[nextIdx])
            },
        )
    }

    GamepadActionCard(
        title = stringResource(R.string.macropad_editor_edit_layout_title),
        description = stringResource(R.string.macropad_editor_edit_layout_desc),
        icon = Icons.Rounded.Edit,
        enabled = activeLayout != null,
        onClick = onEditLayout,
    )

    GamepadActionCard(
        title = stringResource(R.string.settings_macropad_new_layout),
        description = stringResource(R.string.macropad_editor_new_layout_desc),
        icon = Icons.Rounded.Add,
        onClick = onNewLayout,
    )

    GamepadActionCard(
        title = stringResource(R.string.macropad_duplicate_layout),
        description = stringResource(R.string.macropad_editor_duplicate_layout_desc),
        icon = Icons.Rounded.ContentCopy,
        enabled = activeLayout != null,
        onClick = {
            onDuplicateLayout()
            scope.launch {
                try {
                    firstItemFocusRequester.requestFocus()
                } catch (_: IllegalStateException) {
                }
            }
        },
    )

    GamepadActionCard(
        title = stringResource(R.string.macropad_editor_copy_profile_select),
        description = stringResource(R.string.macropad_editor_copy_layout_desc),
        icon = Icons.Rounded.Share,
        enabled = activeLayout != null,
        onClick = onCopyLayout,
    )

    GamepadActionCard(
        title = stringResource(R.string.macropad_reorder_layouts),
        description = stringResource(R.string.macropad_editor_reorder_layouts_desc),
        icon = Icons.Rounded.SwapVert,
        enabled = layouts.size > 1,
        onClick = onReorderLayouts,
    )
}

@Composable
private fun describePadButton(
    btn: PadButton,
    includeHaptic: Boolean = true,
): String {
    val hapticLabel = if (includeHaptic) stringResource(btn.hapticStrength.labelResId()) else null
    return if (btn.action is PadAction.TrackpointMove) {
        val sizeLabel = stringResource((btn.action as PadAction.TrackpointMove).size.labelResId())
        listOfNotNull(sizeLabel, hapticLabel).joinToString(" • ")
    } else {
        val actionLabel = btn.action.displayLabel()
        val sizeLabel =
            if (btn.action !is PadAction.ScrollWheel) {
                "${btn.buttonSize.cols}×${btn.buttonSize.rows}"
            } else {
                null
            }
        listOfNotNull(actionLabel, sizeLabel, hapticLabel).joinToString(" • ")
    }
}

@Composable
private fun ButtonsDeck(
    profile: PadProfile,
    layout: PadLayout?,
    accentColor: Color,
    onEditButtonPositions: () -> Unit,
    onAddButton: () -> Unit,
    onEditButton: (PadButton) -> Unit,
) {
    val colors = LocalAppColors.current
    val rawButtons = layout?.buttons ?: emptyList()
    val isEditingPositions by MacroPadState.isEditingButtonPositions.collectAsStateWithLifecycle()
    val gridMode by MacroPadState.gridMode.collectAsStateWithLifecycle()
    val isGridMode = layout?.isGridMode == true
    val buttons =
        remember(rawButtons, isGridMode) {
            if (isGridMode) {
                rawButtons.sortedWith(compareBy({ it.gridRow ?: 0 }, { it.gridCol ?: 0 }))
            } else {
                rawButtons
            }
        }
    var isReordering by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    var movingItemKey by remember { mutableStateOf<Any?>(null) }
    val movingIndex = if (movingItemKey != null) buttons.indexOfFirst { it.id == movingItemKey } else -1
    val hasFreeCell = !isGridMode || (layout != null && GridLayoutMath.firstFreeCell(layout) != null)
    val headerCount = if (isGridMode) (if (hasFreeCell) 2 else 1) else MPE_BUTTON_HEADER_COUNT

    val reorderState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            if (layout != null && buttons.isNotEmpty() && !isGridMode) {
                val fromButtonIdx = (from.index - headerCount).coerceIn(0, buttons.lastIndex)
                val toButtonIdx = (to.index - headerCount).coerceIn(0, buttons.lastIndex)
                if (fromButtonIdx != toButtonIdx) {
                    val mutable = layout.buttons.toMutableList()
                    mutable.add(toButtonIdx, mutable.removeAt(fromButtonIdx))
                    MacroPadState.updateLayout(layout.copy(buttons = mutable))
                }
            }
        }

    val selectedButtonId by MacroPadState.selectedButtonId.collectAsStateWithLifecycle()

    LaunchedEffect(movingItemKey, movingIndex) {
        if (movingItemKey != null && movingIndex >= 0) {
            lazyListState.animateScrollToItem(movingIndex + headerCount)
        }
    }

    LaunchedEffect(selectedButtonId, buttons) {
        val targetId = selectedButtonId ?: return@LaunchedEffect
        val targetIdx = buttons.indexOfFirst { it.id == targetId }
        if (targetIdx >= 0 && movingItemKey == null) {
            val targetListIndex = targetIdx + headerCount
            lazyListState.animateScrollToItem(targetListIndex)
        }
    }

    LazyColumn(
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(MPE_DECK_SPACING),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            GamepadActionCard(
                title = stringResource(R.string.macropad_editor_edit_button_positions),
                description = stringResource(R.string.macropad_editor_edit_button_positions_card_desc),
                icon = Icons.Rounded.OpenWith,
                onClick = onEditButtonPositions,
                modifier = Modifier.firstDeckItem(),
                onFocusChanged = { if (it) MacroPadState.setSelectedButtonId(null) },
            )
        }

        if (!isGridMode) {
            item {
                val gridModes = GridMode.entries
                GamepadChoiceCard(
                    title = stringResource(R.string.macropad_editor_snap_grid),
                    description = stringResource(R.string.macropad_editor_snap_grid_desc),
                    selectedText = stringResource(gridMode.labelResId()),
                    icon = Icons.Rounded.Grid4x4,
                    onPrevious = { MacroPadState.setGridMode(gridModes.cycle(gridMode, BumperDirection.PREV)) },
                    onNext = { MacroPadState.setGridMode(gridModes.cycle(gridMode, BumperDirection.NEXT)) },
                    onFocusChanged = { if (it) MacroPadState.setSelectedButtonId(null) },
                )
            }
        }

        if (hasFreeCell) {
            item {
                GamepadActionCard(
                    title = stringResource(R.string.macropad_editor_add_button),
                    description = stringResource(R.string.macropad_editor_create_button_desc),
                    icon = Icons.Rounded.Add,
                    onClick = onAddButton,
                    onFocusChanged = { if (it) MacroPadState.setSelectedButtonId(null) },
                )
            }
        }

        if (!isGridMode) {
            item {
                GamepadSectionHeader(
                    text = stringResource(R.string.macropad_editor_manage_buttons),
                    color = accentColor,
                )
            }

            item {
                GamepadToggleCard(
                    title = stringResource(R.string.macropad_editor_reorder_buttons),
                    description =
                        if (isReordering) {
                            stringResource(R.string.macropad_editor_reorder_buttons_enabled_desc)
                        } else {
                            stringResource(R.string.macropad_editor_reorder_buttons_disabled_desc)
                        },
                    checked = isReordering,
                    icon = Icons.Rounded.SwapVert,
                    onCheckedChange = {
                        isReordering = it
                        if (!it) movingItemKey = null
                    },
                    onFocusChanged = { if (it) MacroPadState.setSelectedButtonId(null) },
                )
            }
        }

        if (buttons.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.macropad_editor_no_buttons_in_layout),
                    color = colors.onSurfaceSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = MPE_EMPTY_PADDING_V),
                )
            }
        } else if (!isReordering) {
            items(buttons, key = { it.id }) { btn ->
                GamepadActionCard(
                    title = btn.label.ifBlank { btn.action.displayLabel() },
                    description = describePadButton(btn),
                    icon = btn.action.toCategory().icon(),
                    actionLeadingContent = {
                        PadButtonTrailingIcon(btn = btn, layout = layout, accentColor = accentColor)
                    },
                    itemKey = btn.id,
                    onClick = { onEditButton(btn) },
                    onFocusChanged = { isFocused ->
                        if (isFocused) {
                            MacroPadState.setSelectedButtonId(btn.id)
                        }
                    },
                )
            }
        } else {
            itemsIndexed(buttons, key = { _, btn -> btn.id }) { index, btn ->
                val key = btn.id
                ReorderableItem(reorderState, key = key) { isDragging ->
                    val isMoving = movingItemKey == key
                    val desc = describePadButton(btn, includeHaptic = false)

                    GamepadReorderCard(
                        title = btn.label.ifBlank { btn.action.displayLabel() },
                        description = desc,
                        icon = btn.action.toCategory().icon(),
                        index = index,
                        totalCount = buttons.size,
                        isMoving = isMoving,
                        isDragging = isDragging,
                        onToggleMoving = {
                            movingItemKey = if (isMoving) null else key
                        },
                        onMoveUp = { swapButtons(layout, index, index - 1) },
                        onMoveDown = { swapButtons(layout, index, index + 1) },
                        dragHandleModifier = Modifier.draggableHandle(),
                        itemKey = key,
                        onFocusChanged = { isFocused ->
                            if (isFocused || isMoving) {
                                MacroPadState.setSelectedButtonId(btn.id)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EditButtonPositionsSubPageContent(
    layout: PadLayout?,
    accentColor: Color,
) {
    val colors = LocalAppColors.current
    val rawButtons = layout?.buttons ?: emptyList()
    val isGridMode = layout?.isGridMode == true
    val buttons =
        remember(rawButtons, isGridMode) {
            if (isGridMode) {
                rawButtons.sortedWith(compareBy({ it.gridRow ?: 0 }, { it.gridCol ?: 0 }))
            } else {
                rawButtons
            }
        }
    val coroutineScope = rememberCoroutineScope()
    val selectedButtonId by MacroPadState.selectedButtonId.collectAsStateWithLifecycle()
    val cardRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val movingButtonId by MacroPadState.movingButtonId.collectAsStateWithLifecycle()
    var activeRepeatJob by remember { mutableStateOf<Job?>(null) }
    var activeDirectionKey by remember { mutableIntStateOf(0) }
    var precisionMovement by remember { mutableStateOf(false) }

    fun stopMovingImmediate() {
        activeRepeatJob?.cancel()
        activeRepeatJob = null
        activeDirectionKey = 0
        MacroPadState.setMovingButtonId(null)
    }

    // Intercept system back gesture/button when moving
    BackHandler(enabled = movingButtonId != null) {
        stopMovingImmediate()
    }

    LaunchedEffect(buttons) {
        if (selectedButtonId == null && buttons.isNotEmpty()) {
            MacroPadState.setSelectedButtonId(buttons.first().id)
        }
    }

    LaunchedEffect(selectedButtonId, movingButtonId, buttons) {
        val targetId = movingButtonId ?: selectedButtonId ?: return@LaunchedEffect
        try {
            cardRequesters[targetId]?.requestFocus()
        } catch (_: IllegalStateException) {
            // Focus requester unattached
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopMovingImmediate()
            MacroPadState.setSelectedButtonId(null)
        }
    }

    fun moveButton(
        btnId: String,
        dx: Int,
        dy: Int,
    ) {
        val currentLayout = MacroPadState.previewLayout.value ?: MacroPadState.activeLayout.value ?: return
        val targetBtn = currentLayout.buttons.firstOrNull { it.id == btnId } ?: return

        if (currentLayout.isGridMode) {
            val fromCol = targetBtn.gridCol ?: 0
            val fromRow = targetBtn.gridRow ?: 0
            val cols = currentLayout.effectiveGridCols
            val rows = currentLayout.effectiveGridRows
            val toCol = (fromCol + dx).coerceIn(0, cols - targetBtn.effectiveColSpan)
            val toRow = (fromRow + dy).coerceIn(0, rows - targetBtn.effectiveRowSpan)
            if (toCol != fromCol || toRow != fromRow) {
                val movedLayout = GridLayoutMath.swapOrMoveButton(currentLayout, fromCol to fromRow, toCol to toRow)
                MacroPadState.updateLayout(movedLayout)
                MacroPadState.setPreviewLayout(movedLayout)
            }
        } else {
            val stepX = if (precisionMovement) (1f / MPE_CANVAS_WIDTH_PX) else MPE_MOVE_STEP_NORMALIZED
            val stepY = if (precisionMovement) (1f / MPE_CANVAS_HEIGHT_PX) else MPE_MOVE_STEP_NORMALIZED
            val newX = (targetBtn.posX + dx * stepX).coerceIn(MPE_EDGE_MARGIN, 1f - MPE_EDGE_MARGIN)
            val newY = (targetBtn.posY + dy * stepY).coerceIn(MPE_EDGE_MARGIN, 1f - MPE_EDGE_MARGIN)
            if (newX != targetBtn.posX || newY != targetBtn.posY) {
                val updated =
                    currentLayout.buttons.map {
                        if (it.id == btnId) it.copy(posX = newX, posY = newY) else it
                    }
                val updatedLayout = currentLayout.copy(buttons = updated)
                MacroPadState.updateLayout(updatedLayout)
                MacroPadState.setPreviewLayout(updatedLayout)
            }
        }
    }

    fun startMoving(
        btnId: String,
        keyCode: Int,
        dx: Int,
        dy: Int,
    ) {
        if (activeDirectionKey == keyCode && activeRepeatJob?.isActive == true) return
        activeRepeatJob?.cancel()
        activeDirectionKey = keyCode
        moveButton(btnId, dx, dy)
        activeRepeatJob =
            coroutineScope.launchDirectionalRepeat(
                keyCode = keyCode,
                isActiveCheck = { activeDirectionKey == keyCode },
            ) {
                moveButton(btnId, dx, dy)
            }
    }

    fun stopMoving(keyCode: Int) {
        if (activeDirectionKey == keyCode) {
            activeRepeatJob?.cancel()
            activeRepeatJob = null
            activeDirectionKey = 0
        }
    }

    // Non-highlightable Info Box
    GamepadInfoBox(
        text = stringResource(R.string.macropad_editor_move_buttons_info),
        iconTint = accentColor,
    )

    if (layout?.isGridMode != true) {
        GamepadToggleCard(
            title = stringResource(R.string.macropad_editor_precision_movement_title),
            description = stringResource(R.string.macropad_editor_precision_movement_desc),
            icon = Icons.Rounded.Tune,
            checked = precisionMovement,
            onCheckedChange = { precisionMovement = it },
        )
    }

    if (buttons.isEmpty()) {
        Text(
            text = stringResource(R.string.macropad_editor_no_buttons_in_layout),
            color = colors.onSurfaceSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = MPE_EMPTY_PADDING_V),
        )
    } else {
        buttons.forEachIndexed { index, btn ->
            val cardRequester = remember(btn.id) { FocusRequester() }
            DisposableEffect(btn.id) {
                cardRequesters[btn.id] = cardRequester
                onDispose {
                    cardRequesters.remove(btn.id)
                }
            }
            val isMoving = movingButtonId == btn.id
            val desc = describePadButton(btn)

            GamepadFocusCard(
                cardFocusRequester = cardRequester,
                onClick = {
                    if (isMoving) {
                        stopMovingImmediate()
                    } else {
                        MacroPadState.setSelectedButtonId(btn.id)
                        MacroPadState.setMovingButtonId(btn.id)
                    }
                },
                itemKey = btn.id,
                modifier = Modifier.firstDeckItem(index == 0),
                isAdjusting = isMoving,
                onFocusChanged = { isFocused ->
                    if (isFocused) {
                        if (movingButtonId == null) {
                            MacroPadState.setSelectedButtonId(btn.id)
                        } else if (movingButtonId != btn.id) {
                            try {
                                cardRequesters[movingButtonId]?.requestFocus()
                            } catch (_: IllegalStateException) {}
                        }
                    }
                },
                onCustomKeyEvent = { keyEvent ->
                    if (isMoving) {
                        handle2DAdjustmentKeyEvent(
                            keyEvent = keyEvent,
                            isAdjusting = true,
                            onStartAdjusting = { keyCode, dirX, dirY -> startMoving(btn.id, keyCode, dirX, dirY) },
                            onStopAdjusting = { keyCode -> stopMoving(keyCode) },
                            onDismissAdjustment = {
                                stopMovingImmediate()
                                MacroPadState.setMovingButtonId(null)
                            },
                        )
                    } else if (isGridMode && layout != null && keyEvent.type == KeyEventType.KeyDown) {
                        val keyCode = keyEvent.nativeKeyEvent.keyCode
                        val (dirX, dirY) =
                            when (keyCode) {
                                KeyEvent.KEYCODE_DPAD_UP -> 0 to -1
                                KeyEvent.KEYCODE_DPAD_DOWN -> 0 to 1
                                KeyEvent.KEYCODE_DPAD_LEFT -> -1 to 0
                                KeyEvent.KEYCODE_DPAD_RIGHT -> 1 to 0
                                else -> 0 to 0
                            }
                        if (dirX != 0 || dirY != 0) {
                            val neighbor = GridLayoutMath.findNeighborButton(layout, btn, dirX, dirY)
                            if (neighbor != null) {
                                MacroPadState.setSelectedButtonId(neighbor.id)
                                try {
                                    cardRequesters[neighbor.id]?.requestFocus()
                                } catch (_: IllegalStateException) {}
                                true
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                },
            ) { isFocused ->
                GamepadCardRow(
                    title = btn.label.ifBlank { btn.action.displayLabel() },
                    description = desc,
                    icon = btn.action.toCategory().icon(),
                    trailingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PadButtonTrailingIcon(btn = btn, layout = layout, accentColor = accentColor)
                            if (isMoving) {
                                GamepadPill(
                                    text = stringResource(R.string.gamepad_action_moving),
                                    isAccent = true,
                                )
                            } else {
                                GamepadPill(
                                    text = stringResource(R.string.gamepad_action_move),
                                    isHighlighted = isFocused,
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PadButtonTrailingIcon(
    btn: PadButton,
    layout: PadLayout?,
    accentColor: Color,
) {
    val colors = LocalAppColors.current
    val isTable = layout?.isGridMode == true || layout?.layoutMode == PadLayoutMode.GRID
    val isIconOnly = btn.buttonShape == ButtonShape.ICON_ONLY && !isTable

    val (btnWidth, btnHeight) =
        if (isTable) {
            val cellAspect =
                GridLayoutMath.cellAspectRatio(
                    cols = layout?.effectiveGridCols ?: 1,
                    rows = layout?.effectiveGridRows ?: 1,
                    colSpan = btn.colSpan,
                    rowSpan = btn.rowSpan,
                )
            val baseHeight = 26.dp
            val w = (baseHeight.value * cellAspect).coerceIn(16f, 52f).dp
            w to baseHeight
        } else {
            val baseUnit = 26.dp
            val maxUnit = 38.dp
            val w = (baseUnit.value * btn.buttonSize.cols).coerceIn(16f, maxUnit.value).dp
            val h = (baseUnit.value * btn.buttonSize.rows).coerceIn(16f, maxUnit.value).dp
            w to h
        }

    val chipShape =
        if (isTable) {
            RoundedCornerShape(4.dp)
        } else {
            when (btn.buttonShape) {
                ButtonShape.SQUARE, ButtonShape.ICON_ONLY -> RoundedCornerShape(4.dp)
                ButtonShape.CIRCLE -> {
                    when (btn.buttonSize) {
                        ButtonSize.SIZE_2X2, ButtonSize.SIZE_1X1 -> CircleShape
                        ButtonSize.SIZE_2X1, ButtonSize.SIZE_1X2 -> RoundedCornerShape(4.dp)
                    }
                }
            }
        }

    val resolvedTextColorOption = btn.buttonTextColor ?: layout?.buttonTextColor ?: ColorOption.Neutral
    val effectiveTextTint = resolveColorOption(resolvedTextColorOption, accentColor, colors.onSurface)

    val resolvedBorderColorOption = btn.buttonBorderColor ?: layout?.buttonBorderColor
    val effectiveBorder =
        if (isIconOnly) {
            Color.Transparent
        } else if (resolvedBorderColorOption != null) {
            resolveColorOption(resolvedBorderColorOption, accentColor, colors.controlOverlayBorder)
        } else {
            accentColor.copy(alpha = 0.35f)
        }

    val resolvedBgColorOption = btn.buttonBgColor ?: layout?.buttonBgColor
    val effectiveBg =
        if (isIconOnly) {
            Color.Transparent
        } else if (resolvedBgColorOption != null) {
            resolveColorOption(resolvedBgColorOption, accentColor, Color(0xFF161616))
        } else {
            Color(0xFF161616)
        }

    PadButtonFace(
        width = btnWidth,
        height = btnHeight,
        shape = chipShape,
        isIconOnly = isIconOnly,
        isDeviceDisabled = false,
        borderColor = effectiveBorder,
        bgColor = effectiveBg,
    ) {
        PadButtonContent(
            btn = btn,
            effectiveTextTint = effectiveTextTint,
            iconSize = (minOf(btnWidth, btnHeight).value * 0.6f).dp,
            faceSize = minOf(btnWidth, btnHeight),
            isTrackpoint = btn.action is PadAction.TrackpointMove,
            effectiveContentAccent = accentColor,
            isTableLayout = isTable,
            width = btnWidth,
            height = btnHeight,
            labelOverlay = false,
        )
    }
}

@Composable
private fun MacrosDeck(
    profile: PadProfile,
    accentColor: Color,
    onNewMacro: () -> Unit,
    onEditMacro: (Macro) -> Unit,
    onDeleteMacro: (Macro) -> Unit,
) {
    val privdState by PrivdManager.state.collectAsStateWithLifecycle()
    val isPrivdRunning = privdState == PrivdState.RUNNING

    if (!isPrivdRunning) {
        GamepadInfoBox(
            text = stringResource(R.string.macropad_macro_privd_required_banner),
            modifier = Modifier.firstDeckItem(),
        )
        return
    }

    GamepadActionCard(
        title = stringResource(R.string.macropad_editor_open_timeline_title),
        description = stringResource(R.string.macropad_editor_open_timeline_desc),
        icon = Icons.Rounded.Add,
        onClick = onNewMacro,
        modifier = Modifier.firstDeckItem(),
    )

    val macros = profile.macros
    if (macros.isEmpty()) {
        GamepadInfoBox(
            text = stringResource(R.string.macropad_editor_no_macros_desc),
        )
    } else {
        macros.forEach { macro ->
            val stepCountDesc =
                if (macro.steps.size == 1) {
                    stringResource(R.string.macropad_macro_step_count_single)
                } else {
                    stringResource(R.string.macropad_macro_step_count_multiple, macro.steps.size)
                }
            GamepadActionCard(
                title = macro.name,
                description = stepCountDesc,
                icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                onClick = { onEditMacro(macro) },
            )
        }
    }
}
