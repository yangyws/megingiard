package com.stormpanda.megingiard.macropad

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stormpanda.megingiard.AppLog
import com.stormpanda.megingiard.BitmapUtils
import com.stormpanda.megingiard.R
import com.stormpanda.megingiard.settings.SettingsManager
import com.stormpanda.megingiard.ui.BumperDirection
import com.stormpanda.megingiard.ui.GamepadActionCard
import com.stormpanda.megingiard.ui.GamepadChoiceCard
import com.stormpanda.megingiard.ui.GamepadSaveExitActionRow
import com.stormpanda.megingiard.ui.GamepadSectionHeader
import com.stormpanda.megingiard.ui.GamepadToggleCard
import com.stormpanda.megingiard.ui.GamepadTwoStepConfirmCard
import com.stormpanda.megingiard.ui.LocalAppColors
import com.stormpanda.megingiard.ui.cycle
import com.stormpanda.megingiard.ui.firstDeckItem
import com.stormpanda.megingiard.ui.rememberSaveExitPromptState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "ButtonCustomImageEditor"

private val BCIE_PREVIEW_PADDING = 12.dp
private val BCIE_PREVIEW_IMAGE_ROUNDING = 8.dp
private val BCIE_PREVIEW_SHAPE = RoundedCornerShape(BCIE_PREVIEW_IMAGE_ROUNDING)
private val BCIE_ICON_SIZE_48 = 48.dp
private const val BCIE_PREVIEW_WIDTH_FRACTION = 0.5f

private fun BackgroundScaleMode.labelResId(): Int =
    when (this) {
        BackgroundScaleMode.FILL -> R.string.bg_scale_mode_fill
        BackgroundScaleMode.FIT -> R.string.bg_scale_mode_fit
        BackgroundScaleMode.STRETCH -> R.string.bg_scale_mode_stretch
    }

private fun BackgroundScaleMode.descriptionResId(): Int =
    when (this) {
        BackgroundScaleMode.FILL -> R.string.bg_scale_mode_fill_desc
        BackgroundScaleMode.FIT -> R.string.bg_scale_mode_fit_desc
        BackgroundScaleMode.STRETCH -> R.string.bg_scale_mode_stretch_desc
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ButtonCustomImageSubPageContent(
    button: PadButton?,
    draftButton: PadButton,
    activeLayout: PadLayout?,
    accentColor: Color,
    onOpenScrape: (currentDraft: PadButton) -> Unit,
    onOpenPicker: (currentDraft: PadButton) -> Unit,
    onDiscard: () -> Unit = {},
    onConfirm: (updatedDraft: PadButton) -> Unit,
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current

    var imageAssetId by remember(draftButton) { mutableStateOf(draftButton.imageAssetId) }
    var imageScaleMode by remember(draftButton) { mutableStateOf(draftButton.imageScaleMode) }
    var imageScale by remember(draftButton) { mutableFloatStateOf(draftButton.imageScale) }
    var imageOffsetX by remember(draftButton) { mutableFloatStateOf(draftButton.imageOffsetX) }
    var imageOffsetY by remember(draftButton) { mutableFloatStateOf(draftButton.imageOffsetY) }
    var showLabel by remember(draftButton) { mutableStateOf(draftButton.showLabel) }
    var showLabelBg by remember(draftButton) { mutableStateOf(draftButton.showLabelBg) }
    var enlargeText by remember(draftButton) { mutableStateOf(draftButton.enlargeText) }
    var previewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(imageAssetId) {
        val assetId = imageAssetId
        if (assetId != null) {
            withContext(Dispatchers.IO) {
                val loaded = PadIconStore.load(context, assetId)
                withContext(Dispatchers.Main) {
                    previewBitmap = loaded?.asImageBitmap()
                }
            }
        } else {
            previewBitmap = null
        }
    }

    val isGrid = activeLayout?.isGridMode == true
    val buttonCropAspect =
        if (isGrid) {
            GridLayoutMath.cellAspectRatio(
                cols = activeLayout?.effectiveGridCols ?: 1,
                rows = activeLayout?.effectiveGridRows ?: 1,
                colSpan = draftButton.colSpan,
                rowSpan = draftButton.rowSpan,
            )
        } else {
            (draftButton.buttonSize.cols.toFloat() / draftButton.buttonSize.rows.toFloat().coerceAtLeast(1f)).coerceAtLeast(0.1f)
        }

    val inFlightDraft =
        remember(
            draftButton,
            imageAssetId,
            imageScaleMode,
            imageScale,
            imageOffsetX,
            imageOffsetY,
            showLabel,
            showLabelBg,
            enlargeText,
        ) {
            draftButton.copy(
                imageAssetId = imageAssetId,
                iconName = if (imageAssetId != null) null else draftButton.iconName,
                imageScaleMode = imageScaleMode,
                imageScale = imageScale,
                imageOffsetX = imageOffsetX,
                imageOffsetY = imageOffsetY,
                showLabel = showLabel,
                showLabelBg = showLabelBg,
                enlargeText = enlargeText,
            )
        }

    LaunchedEffect(inFlightDraft) {
        MacroPadState.setPreviewButton(inFlightDraft)
    }

    LaunchedEffect(previewBitmap, buttonCropAspect, inFlightDraft) {
        val bmp = previewBitmap
        if (bmp != null) {
            MacroPadState.setCroppingButtonState(
                CroppingButtonState(
                    button = button,
                    draftButton = inFlightDraft,
                    sourceBitmap = bmp.asAndroidBitmap(),
                    aspectRatio = buttonCropAspect,
                    isGridMode = isGrid,
                    scaleMode = imageScaleMode,
                    scale = imageScale,
                    offsetX = imageOffsetX,
                    offsetY = imageOffsetY,
                ),
            )
        } else {
            MacroPadState.setCroppingButtonState(null)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            MacroPadState.setCroppingButtonState(null)
        }
    }

    val croppingState by MacroPadState.croppingButtonState.collectAsState()
    LaunchedEffect(croppingState?.scale, croppingState?.offsetX, croppingState?.offsetY, croppingState?.scaleMode) {
        val cs = croppingState ?: return@LaunchedEffect
        imageScale = cs.scale
        imageOffsetX = cs.offsetX
        imageOffsetY = cs.offsetY
        imageScaleMode = cs.scaleMode
    }

    LaunchedEffect(Unit) {
        ButtonImagePickerManager.pickedUriFlow.collect { uri ->
            ButtonImagePickerManager.clearPickedUri()
            AppLog.i(TAG, "Received picked button image uri from system picker: $uri")
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
                    imageAssetId = savedAssetId
                    imageScaleMode = BackgroundScaleMode.FILL
                    imageScale = 1.0f
                    imageOffsetX = 0f
                    imageOffsetY = 0f
                    previewBitmap = decoded.asImageBitmap()
                }
            }
        }
    }

    val layoutTextOpt = activeLayout?.buttonTextColor ?: ColorOption.Neutral
    val layoutBorderOpt = activeLayout?.buttonBorderColor ?: ColorOption.Neutral
    val layoutBgOpt = activeLayout?.buttonBgColor ?: ColorOption.Neutral

    val effectiveTextOpt = inFlightDraft.buttonTextColor ?: layoutTextOpt
    val effectiveBorderOpt = inFlightDraft.buttonBorderColor ?: layoutBorderOpt
    val effectiveBgOpt = inFlightDraft.buttonBgColor ?: layoutBgOpt

    val effectiveTextTint = resolveColorOption(effectiveTextOpt, accentColor, MP_AMBIENT_NEUTRAL_TEXT)
    val effectiveBorder = resolveColorOption(effectiveBorderOpt, accentColor, MP_AMBIENT_NEUTRAL_BORDER)
    val effectiveBg =
        when (effectiveBgOpt) {
            ColorOption.Neutral -> Color(0xFF161616)
            ColorOption.Accent -> accentColor.copy(alpha = 0.35f)
            is ColorOption.Custom -> Color(effectiveBgOpt.argb)
        }

    // ── Preview Card at Top ───────────────────────────────────────────
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = BCIE_PREVIEW_PADDING, vertical = BCIE_PREVIEW_PADDING),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(BCIE_PREVIEW_WIDTH_FRACTION)
                    .aspectRatio(buttonCropAspect)
                    .clip(BCIE_PREVIEW_SHAPE)
                    .background(effectiveBg)
                    .border(
                        width = 2.dp,
                        color = effectiveBorder,
                        shape = BCIE_PREVIEW_SHAPE,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            val bmp = previewBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = stringResource(R.string.button_settings_custom_image),
                    contentScale =
                        when (imageScaleMode) {
                            BackgroundScaleMode.FILL -> ContentScale.Crop
                            BackgroundScaleMode.FIT -> ContentScale.Fit
                            BackgroundScaleMode.STRETCH -> ContentScale.FillBounds
                        },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Image,
                    contentDescription = null,
                    tint = effectiveTextTint.copy(alpha = 0.5f),
                    modifier = Modifier.size(BCIE_ICON_SIZE_48),
                )
            }

            val textLabel = inFlightDraft.label.ifBlank { inFlightDraft.action.displayLabel() }
            if (showLabel && textLabel.isNotBlank()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(bottom = 4.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    val labelBgModifier =
                        if (showLabelBg) {
                            Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        }
                    Box(
                        modifier = labelBgModifier,
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = textLabel,
                            color = effectiveTextTint,
                            fontSize = if (enlargeText) 16.sp else 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }

    // ── 1. 圖片來源 (Artwork Source) ──────────────────────────────────
    GamepadSectionHeader(
        text = stringResource(R.string.macropad_editor_section_artwork_source),
        color = accentColor,
    )

    GamepadActionCard(
        title = stringResource(R.string.layout_settings_bg_image_browse_local),
        description = stringResource(R.string.button_image_picker_desc),
        icon = Icons.Rounded.Folder,
        onClick = { onOpenPicker(inFlightDraft) },
        modifier = Modifier.firstDeckItem(),
    )

    GamepadActionCard(
        title = stringResource(R.string.layout_settings_bg_image_scrape),
        description = stringResource(R.string.macropad_editor_bg_steamgriddb_desc),
        icon = Icons.Rounded.Search,
        onClick = {
            if (SettingsManager.steamGridDbApiToken.value.isBlank()) {
                Toast.makeText(context, R.string.steamgriddb_token_missing_message, Toast.LENGTH_LONG).show()
            } else {
                onOpenScrape(inFlightDraft)
            }
        },
    )

    // ── 2. 圖片調整 (Image Adjustments) ──────────────────────────────
    if (previewBitmap != null) {
        GamepadSectionHeader(
            text = stringResource(R.string.layout_settings_bg_image_adjustments),
            color = accentColor,
        )

        GamepadChoiceCard(
            title = stringResource(R.string.layout_settings_bg_scale_mode),
            description = stringResource(imageScaleMode.descriptionResId()),
            selectedText = stringResource(imageScaleMode.labelResId()),
            icon = Icons.Rounded.AspectRatio,
            onPrevious = {
                val newMode = BackgroundScaleMode.entries.cycle(imageScaleMode, BumperDirection.PREV)
                imageScaleMode = newMode
                MacroPadState.updateCroppingButtonScaleMode(newMode)
            },
            onNext = {
                val newMode = BackgroundScaleMode.entries.cycle(imageScaleMode, BumperDirection.NEXT)
                imageScaleMode = newMode
                MacroPadState.updateCroppingButtonScaleMode(newMode)
            },
            modifier = Modifier.firstDeckItem(),
        )

        GamepadToggleCard(
            title = stringResource(R.string.button_settings_show_label),
            description = stringResource(R.string.button_settings_show_label_desc),
            checked = showLabel,
            onCheckedChange = { showLabel = it },
        )

        if (showLabel) {
            GamepadToggleCard(
                title = stringResource(R.string.button_settings_show_label_bg),
                description = stringResource(R.string.button_settings_show_label_bg_desc),
                checked = showLabelBg,
                onCheckedChange = { showLabelBg = it },
            )
            GamepadToggleCard(
                title = stringResource(R.string.button_settings_enlarge_text),
                description = stringResource(R.string.button_settings_enlarge_text_desc),
                checked = enlargeText,
                onCheckedChange = { enlargeText = it },
            )
        }
    }

    // ── 3. 儲存與刪除 (Save & Delete) ─────────────────────────────────
    GamepadSectionHeader(
        text = stringResource(R.string.macropad_editor_section_save_and_delete),
        color = accentColor,
    )

    val hasChanges =
        imageAssetId != draftButton.imageAssetId ||
            imageScaleMode != draftButton.imageScaleMode ||
            kotlin.math.abs(imageScale - draftButton.imageScale) > 0.001f ||
            kotlin.math.abs(imageOffsetX - draftButton.imageOffsetX) > 0.001f ||
            kotlin.math.abs(imageOffsetY - draftButton.imageOffsetY) > 0.001f ||
            showLabel != draftButton.showLabel ||
            showLabelBg != draftButton.showLabelBg ||
            enlargeText != draftButton.enlargeText

    val promptState =
        rememberSaveExitPromptState(
            hasChanges = hasChanges,
            onSave = {
                MacroPadState.setCroppingButtonState(null)
                val updated =
                    draftButton.copy(
                        imageAssetId = imageAssetId,
                        iconName = if (imageAssetId != null) null else draftButton.iconName,
                        imageScaleMode = imageScaleMode,
                        imageScale = imageScale,
                        imageOffsetX = imageOffsetX,
                        imageOffsetY = imageOffsetY,
                        showLabel = showLabel,
                        showLabelBg = showLabelBg,
                        enlargeText = enlargeText,
                    )
                onConfirm(updated)
            },
            onDiscard = {
                MacroPadState.setCroppingButtonState(null)
                imageAssetId = draftButton.imageAssetId
                imageScaleMode = draftButton.imageScaleMode
                imageScale = draftButton.imageScale
                imageOffsetX = draftButton.imageOffsetX
                imageOffsetY = draftButton.imageOffsetY
                showLabel = draftButton.showLabel
                showLabelBg = draftButton.showLabelBg
                enlargeText = draftButton.enlargeText
                MacroPadState.setPreviewButton(draftButton)
                onDiscard()
            },
        )

    GamepadSaveExitActionRow(
        title = stringResource(R.string.macropad_editor_save_button_title),
        description = stringResource(R.string.macropad_editor_save_button_desc),
        pulseOnChanges = hasChanges,
        saveActionText = stringResource(R.string.gamepad_action_save),
        saveIcon = Icons.Rounded.Save,
        enabled = true,
        showExitPrompt = promptState.showExitPrompt,
        onDismissPrompt = promptState.dismissPrompt,
        saveFocusRequester = promptState.focusRequester,
        bringIntoViewRequester = promptState.bringIntoViewRequester,
        onSave = promptState.onSave,
        onDiscard = promptState.onDiscard,
    )

    GamepadTwoStepConfirmCard(
        title = stringResource(R.string.button_settings_clear_image),
        confirmTitle = stringResource(R.string.gamepad_action_confirm),
        description = stringResource(R.string.button_settings_custom_image_desc_selected),
        actionText = stringResource(R.string.gamepad_action_clear),
        confirmActionText = stringResource(R.string.gamepad_action_confirm),
        icon = Icons.Rounded.Delete,
        isDestructive = true,
        enabled = imageAssetId != null,
        onConfirm = {
            MacroPadState.setCroppingButtonState(null)
            imageAssetId = null
            imageScaleMode = BackgroundScaleMode.FILL
            imageScale = 1.0f
            imageOffsetX = 0f
            imageOffsetY = 0f
            previewBitmap = null
        },
    )
}
