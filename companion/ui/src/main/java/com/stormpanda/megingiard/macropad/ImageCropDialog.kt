package com.stormpanda.megingiard.macropad

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.stormpanda.megingiard.AppLog
import com.stormpanda.megingiard.R
import com.stormpanda.megingiard.ui.AppModalDialog
import com.stormpanda.megingiard.ui.DragResizeHandle
import com.stormpanda.megingiard.ui.GamepadActionCard
import com.stormpanda.megingiard.ui.GamepadChoiceCard
import com.stormpanda.megingiard.ui.LocalAppColors
import com.stormpanda.megingiard.ui.firstDeckItem
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val TAG = "ImageCropDialog"

private const val CROP_MODAL_WIDTH_FRACTION = 0.85f
private val CROP_MODAL_CORNER_RADIUS = 12.dp
private const val CROP_MODAL_BG_ALPHA = 0.6f
private val CROP_IMAGE_ROUNDING = 8.dp
private const val CROP_OUTPUT_MAX_PX = 512

private val CROP_SPACING_4 = 4.dp
private val CROP_SPACING_8 = 8.dp
private val CROP_SPACING_12 = 12.dp
private val CROP_SPACING_16 = 16.dp
private val CROP_CHIP_HEIGHT = 32.dp

private val CROP_HANDLE_TOUCH_SIZE = 44.dp
private val CROP_HANDLE_INDICATOR_SIZE = 20.dp
private val CROP_HANDLE_CORNER = 4.dp
private val CROP_SELECTION_BORDER = 2.dp
private const val CROP_SCRIM_ALPHA = 0.55f

internal enum class CropFitMode {
    FILL,
    FIT,
    STRETCH,
}

private fun cropOutputSize(aspectRatio: Float): IntSize {
    val ratio = if (aspectRatio.isFinite() && aspectRatio > 0f) aspectRatio else 1f
    return if (ratio >= 1f) {
        IntSize(CROP_OUTPUT_MAX_PX, (CROP_OUTPUT_MAX_PX / ratio).roundToInt().coerceAtLeast(1))
    } else {
        IntSize((CROP_OUTPUT_MAX_PX * ratio).roundToInt().coerceAtLeast(1), CROP_OUTPUT_MAX_PX)
    }
}

internal fun renderCroppedBitmap(
    source: Bitmap,
    aspectRatio: Float,
    selection: CropSelection,
    widthFraction: Float,
    heightFraction: Float,
    fitMode: CropFitMode = CropFitMode.FILL,
): Bitmap? {
    if (source.width <= 0 || source.height <= 0) {
        AppLog.w(TAG, "Cannot crop a ${source.width}x${source.height} source")
        return null
    }
    return try {
        val out = cropOutputSize(aspectRatio)
        val target = Bitmap.createBitmap(out.width, out.height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(target)
        val paint =
            Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
                isDither = true
            }
        val effW = if (fitMode == CropFitMode.STRETCH) 1f else widthFraction
        val effH = if (fitMode == CropFitMode.STRETCH) 1f else heightFraction
        val rect = CropSelectionMath.imageRectInOutput(selection, effW, effH)
        val left = rect[0] * out.width
        val top = rect[1] * out.height
        canvas.drawBitmap(
            source,
            null,
            RectF(left, top, left + rect[2] * out.width, top + rect[3] * out.height),
            paint,
        )
        target
    } catch (e: Exception) {
        AppLog.e(TAG, "Failed to bake cropped bitmap", e)
        null
    }
}

@Composable
internal fun ImageCropDialog(
    bitmap: ImageBitmap,
    aspectRatio: Float,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.layout_settings_crop_image_title),
    initialTransform: CropTransform? = null,
    initialFit: CropFitMode = CropFitMode.FILL,
    showFitToggle: Boolean = false,
    onConfirmCrop: ((scale: Float, offsetX: Float, offsetY: Float) -> Unit)? = null,
    onConfirmBitmap: ((Bitmap) -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    val density = LocalDensity.current

    val extents =
        remember(bitmap, aspectRatio) {
            CropSelectionMath.imageExtents(bitmap.width.toFloat(), bitmap.height.toFloat(), aspectRatio)
        }
    val widthFraction = extents.first
    val heightFraction = extents.second

    var fitMode by remember { mutableStateOf(initialFit) }
    val allowMargins = fitMode == CropFitMode.FIT || !showFitToggle
    val effW = if (fitMode == CropFitMode.STRETCH) 1f else widthFraction
    val effH = if (fitMode == CropFitMode.STRETCH) 1f else heightFraction

    var selection by remember {
        mutableStateOf(
            if (initialTransform != null) {
                CropSelectionMath.fromTransform(initialTransform, effW, effH, allowMargins)
            } else {
                CropSelectionMath.maxSelection(effW, effH, allowMargins)
            },
        )
    }
    var stageSize by remember { mutableStateOf(IntSize.Zero) }
    var dragOrigin by remember { mutableStateOf(selection) }
    var isStageFocused by remember { mutableStateOf(false) }

    val cancelRequester = remember { FocusRequester() }
    val doneRequester = remember { FocusRequester() }
    val stageRequester = remember { FocusRequester() }
    val fillRequester = remember { FocusRequester() }
    val fitRequester = remember { FocusRequester() }
    val stretchRequester = remember { FocusRequester() }

    val cancelInteractionSource = remember { MutableInteractionSource() }
    val isCancelFocused by cancelInteractionSource.collectIsFocusedAsState()
    val doneInteractionSource = remember { MutableInteractionSource() }
    val isDoneFocused by doneInteractionSource.collectIsFocusedAsState()

    BackHandler {
        onDismiss()
    }

    LaunchedEffect(Unit) {
        delay(60)
        try {
            cancelRequester.requestFocus()
        } catch (_: Exception) {
            delay(100)
            try {
                cancelRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    AppModalDialog(
        onDismiss = onDismiss,
        widthFraction = CROP_MODAL_WIDTH_FRACTION,
        cornerRadius = CROP_MODAL_CORNER_RADIUS,
        contentPadding = CROP_SPACING_16,
        scrimAlpha = CROP_MODAL_BG_ALPHA,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onDismiss,
                interactionSource = cancelInteractionSource,
                modifier =
                    Modifier
                        .focusRequester(cancelRequester)
                        .focusProperties {
                            right = doneRequester
                            down = stageRequester
                        }
                        .then(
                            if (isCancelFocused) {
                                Modifier.border(2.dp, colors.accent, RoundedCornerShape(8.dp))
                            } else {
                                Modifier
                            },
                        ),
            ) {
                Text(
                    text = stringResource(R.string.macropad_editor_cancel),
                    color = if (isCancelFocused) colors.accent else colors.onSurfaceSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCancelFocused) FontWeight.Bold else FontWeight.Normal,
                )
            }

            Text(
                text = title,
                color = colors.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )

            TextButton(
                onClick = {
                    val transform = CropSelectionMath.toTransform(selection, effW, effH)
                    onConfirmCrop?.invoke(transform.scale, transform.offsetX, transform.offsetY)

                    if (onConfirmBitmap != null) {
                        val baked =
                            runCatching {
                                renderCroppedBitmap(
                                    source = bitmap.asAndroidBitmap(),
                                    aspectRatio = aspectRatio,
                                    selection = selection,
                                    widthFraction = widthFraction,
                                    heightFraction = heightFraction,
                                    fitMode = fitMode,
                                )
                            }.onFailure { AppLog.e(TAG, "Crop source is not an Android bitmap", it) }
                                .getOrNull()
                        if (baked != null) onConfirmBitmap(baked) else AppLog.w(TAG, "Crop produced no bitmap")
                    }
                },
                interactionSource = doneInteractionSource,
                modifier =
                    Modifier
                        .focusRequester(doneRequester)
                        .focusProperties {
                            left = cancelRequester
                            down = stageRequester
                        }
                        .then(
                            if (isDoneFocused) {
                                Modifier.border(2.dp, colors.accent, RoundedCornerShape(8.dp))
                            } else {
                                Modifier
                            },
                        ),
            ) {
                Text(
                    text = stringResource(R.string.mirror_editor_done),
                    color = colors.accent,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(CROP_SPACING_16))

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(CROP_IMAGE_ROUNDING))
                    .clipToBounds()
                    .background(Color.Black)
                    .focusRequester(stageRequester)
                    .onFocusChanged { isStageFocused = it.isFocused }
                    .focusable()
                    .then(
                        if (isStageFocused) {
                            Modifier.border(2.dp, colors.accent, RoundedCornerShape(CROP_IMAGE_ROUNDING))
                        } else {
                            Modifier
                        },
                    )
                    .focusProperties {
                        up = cancelRequester
                        down = if (showFitToggle) fillRequester else cancelRequester
                    }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            val step = 0.03f
                            val zoomStep = 0.05f
                            when (keyEvent.nativeKeyEvent.keyCode) {
                                AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                                    selection = CropSelectionMath.move(selection, 0f, -step, effW, effH, allowMargins)
                                    true
                                }
                                AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                                    selection = CropSelectionMath.move(selection, 0f, step, effW, effH, allowMargins)
                                    true
                                }
                                AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                                    selection = CropSelectionMath.move(selection, -step, 0f, effW, effH, allowMargins)
                                    true
                                }
                                AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                                    selection = CropSelectionMath.move(selection, step, 0f, effW, effH, allowMargins)
                                    true
                                }
                                AndroidKeyEvent.KEYCODE_BUTTON_L1 -> {
                                    val newSize = (selection.size - zoomStep).coerceAtLeast(CropSelectionMath.minSize(effW, effH))
                                    selection = CropSelectionMath.clamp(selection.copy(size = newSize), effW, effH, allowMargins)
                                    true
                                }
                                AndroidKeyEvent.KEYCODE_BUTTON_R1 -> {
                                    val newSize = (selection.size + zoomStep).coerceAtMost(CropSelectionMath.maxSize(effW, effH, allowMargins))
                                    selection = CropSelectionMath.clamp(selection.copy(size = newSize), effW, effH, allowMargins)
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }
                    .onSizeChanged { stageSize = it }
                    .pointerInput(bitmap, effW, effH, allowMargins) {
                        var travelX = 0f
                        var travelY = 0f
                        detectDragGestures(
                            onDragStart = {
                                dragOrigin = selection
                                travelX = 0f
                                travelY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                if (w <= 0f || h <= 0f) return@detectDragGestures
                                travelX += dragAmount.x
                                travelY += dragAmount.y
                                selection =
                                    CropSelectionMath.move(
                                        dragOrigin,
                                        travelX / w,
                                        travelY / h,
                                        effW,
                                        effH,
                                        allowMargins,
                                    )
                            },
                        )
                    },
        ) {
            val stageW = stageSize.width.toFloat()
            val stageH = stageSize.height.toFloat()

            if (stageW > 0f && stageH > 0f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val drawnW = effW * stageW
                    val drawnH = effH * stageH
                    drawImage(
                        image = bitmap,
                        dstOffset =
                            IntOffset(
                                ((stageW - drawnW) / 2f).roundToInt(),
                                ((stageH - drawnH) / 2f).roundToInt(),
                            ),
                        dstSize = IntSize(drawnW.roundToInt(), drawnH.roundToInt()),
                    )

                    val boxLeft = selection.left * stageW
                    val boxTop = selection.top * stageH
                    val boxW = selection.size * stageW
                    val boxH = selection.size * stageH

                    val scrim = Color.Black.copy(alpha = CROP_SCRIM_ALPHA)
                    drawRect(scrim, Offset(0f, 0f), Size(stageW, boxTop.coerceAtLeast(0f)))
                    drawRect(scrim, Offset(0f, boxTop + boxH), Size(stageW, (stageH - boxTop - boxH).coerceAtLeast(0f)))
                    drawRect(scrim, Offset(0f, boxTop), Size(boxLeft.coerceAtLeast(0f), boxH))
                    drawRect(
                        scrim,
                        Offset(boxLeft + boxW, boxTop),
                        Size((stageW - boxLeft - boxW).coerceAtLeast(0f), boxH),
                    )

                    drawRect(
                        color = colors.accent,
                        topLeft = Offset(boxLeft, boxTop),
                        size = Size(boxW, boxH),
                        style = Stroke(width = CROP_SELECTION_BORDER.toPx()),
                    )
                }

                val touchPx = with(density) { CROP_HANDLE_TOUCH_SIZE.toPx() }
                val gripPx = with(density) { CROP_HANDLE_INDICATOR_SIZE.toPx() }
                val boxLeft = selection.left * stageW
                val boxTop = selection.top * stageH
                val boxRight = boxLeft + selection.size * stageW
                val boxBottom = boxTop + selection.size * stageH

                CropCorner.entries.forEach { corner ->
                    val cornerX = if (corner.isLeft) boxLeft + gripPx / 2f else boxRight - gripPx / 2f
                    val cornerY = if (corner.isTop) boxTop + gripPx / 2f else boxBottom - gripPx / 2f
                    DragResizeHandle(
                        offset =
                            IntOffset(
                                (cornerX - touchPx / 2f).roundToInt(),
                                (cornerY - touchPx / 2f).roundToInt(),
                            ),
                        touchWidth = CROP_HANDLE_TOUCH_SIZE,
                        touchHeight = CROP_HANDLE_TOUCH_SIZE,
                        indicatorSize = CROP_HANDLE_INDICATOR_SIZE,
                        indicatorCorner = CROP_HANDLE_CORNER,
                        color = colors.accent,
                        onDragStart = { dragOrigin = selection },
                        onDrag = { totalX, totalY ->
                            selection =
                                CropSelectionMath.resize(
                                    dragOrigin,
                                    corner,
                                    totalX / stageW,
                                    totalY / stageH,
                                    effW,
                                    effH,
                                    allowMargins,
                                )
                        },
                    )
                }
            }
        }

        if (showFitToggle) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = CROP_SPACING_12),
                horizontalArrangement = Arrangement.spacedBy(CROP_SPACING_8, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CropFitModeChip(
                    label = stringResource(R.string.bg_scale_mode_fill),
                    selected = fitMode == CropFitMode.FILL,
                    modifier =
                        Modifier
                            .focusRequester(fillRequester)
                            .focusProperties {
                                right = fitRequester
                                up = stageRequester
                            },
                    onClick = {
                        if (fitMode != CropFitMode.FILL) {
                            fitMode = CropFitMode.FILL
                            selection = CropSelectionMath.maxSelection(widthFraction, heightFraction, false)
                        }
                    },
                )
                CropFitModeChip(
                    label = stringResource(R.string.bg_scale_mode_fit),
                    selected = fitMode == CropFitMode.FIT,
                    modifier =
                        Modifier
                            .focusRequester(fitRequester)
                            .focusProperties {
                                left = fillRequester
                                right = stretchRequester
                                up = stageRequester
                            },
                    onClick = {
                        if (fitMode != CropFitMode.FIT) {
                            fitMode = CropFitMode.FIT
                            selection = CropSelectionMath.maxSelection(widthFraction, heightFraction, true)
                        }
                    },
                )
                CropFitModeChip(
                    label = stringResource(R.string.bg_scale_mode_stretch),
                    selected = fitMode == CropFitMode.STRETCH,
                    modifier =
                        Modifier
                            .focusRequester(stretchRequester)
                            .focusProperties {
                                left = fitRequester
                                up = stageRequester
                            },
                    onClick = {
                        if (fitMode != CropFitMode.STRETCH) {
                            fitMode = CropFitMode.STRETCH
                            selection = CropSelectionMath.maxSelection(1f, 1f, false)
                        }
                    },
                )
            }
        }
    }
}

private val CropCorner.isLeft: Boolean
    get() = this == CropCorner.TOP_LEFT || this == CropCorner.BOTTOM_LEFT

private val CropCorner.isTop: Boolean
    get() = this == CropCorner.TOP_LEFT || this == CropCorner.TOP_RIGHT

@Composable
private fun CropFitModeChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = if (selected) colors.accent else colors.surfaceVariant,
                contentColor = if (selected) colors.onAccent else colors.onSurface,
            ),
        contentPadding = PaddingValues(horizontal = CROP_SPACING_16, vertical = CROP_SPACING_4),
        modifier =
            modifier
                .height(CROP_CHIP_HEIGHT)
                .then(
                    if (isFocused) {
                        Modifier.border(2.dp, colors.accent, RoundedCornerShape(20.dp))
                    } else {
                        Modifier
                    },
                ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}

/**
 * Full deck sub-page content for image cropping in [MacroPadEditor].
 */
@Composable
internal fun ImageCropSubPageContent(
    bitmap: ImageBitmap,
    aspectRatio: Float,
    onConfirm: (baked: Bitmap) -> Unit,
    onCancel: () -> Unit,
    initialTransform: CropTransform? = null,
    initialFit: CropFitMode = CropFitMode.FILL,
    showFitToggle: Boolean = true,
) {
    val colors = LocalAppColors.current
    val density = LocalDensity.current

    val extents =
        remember(bitmap, aspectRatio) {
            CropSelectionMath.imageExtents(bitmap.width.toFloat(), bitmap.height.toFloat(), aspectRatio)
        }
    val widthFraction = extents.first
    val heightFraction = extents.second

    var fitMode by remember { mutableStateOf(initialFit) }
    val allowMargins = fitMode == CropFitMode.FIT || !showFitToggle
    val effW = if (fitMode == CropFitMode.STRETCH) 1f else widthFraction
    val effH = if (fitMode == CropFitMode.STRETCH) 1f else heightFraction

    var selection by remember {
        mutableStateOf(
            if (initialTransform != null) {
                CropSelectionMath.fromTransform(initialTransform, effW, effH, allowMargins)
            } else {
                CropSelectionMath.maxSelection(effW, effH, allowMargins)
            },
        )
    }
    var stageSize by remember { mutableStateOf(IntSize.Zero) }
    var dragOrigin by remember { mutableStateOf(selection) }
    val currentSelection = rememberUpdatedState(selection)
    val zoomStep = 0.05f

    BackHandler(onBack = onCancel)

    // Global bumper controls for zooming
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            AndroidKeyEvent.KEYCODE_BUTTON_L1 -> {
                                val sel = currentSelection.value
                                val newSize = (sel.size - zoomStep).coerceAtLeast(CropSelectionMath.minSize(effW, effH))
                                selection = CropSelectionMath.clamp(sel.copy(size = newSize), effW, effH, allowMargins)
                                true
                            }
                            AndroidKeyEvent.KEYCODE_BUTTON_R1 -> {
                                val sel = currentSelection.value
                                val newSize = (sel.size + zoomStep).coerceAtMost(CropSelectionMath.maxSize(effW, effH, allowMargins))
                                selection = CropSelectionMath.clamp(sel.copy(size = newSize), effW, effH, allowMargins)
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                },
    ) {
        // 1. Crop Preview Stage Card
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val stageAspect = if (aspectRatio.isFinite() && aspectRatio > 0f) aspectRatio else 1f
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(CROP_MODAL_WIDTH_FRACTION)
                        .aspectRatio(stageAspect.coerceIn(0.5f, 2f))
                        .clip(RoundedCornerShape(CROP_IMAGE_ROUNDING))
                        .clipToBounds()
                        .background(Color.Black)
                        .border(1.dp, colors.surfaceVariant, RoundedCornerShape(CROP_IMAGE_ROUNDING))
                        .onSizeChanged { stageSize = it }
                        .pointerInput(bitmap, effW, effH, allowMargins) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                if (w <= 0f || h <= 0f) return@detectTransformGestures
                                var cur = currentSelection.value
                                if (zoom != 1f) {
                                    val currentSize = cur.size
                                    val targetSize =
                                        (currentSize / zoom).coerceIn(
                                            CropSelectionMath.minSize(effW, effH),
                                            CropSelectionMath.maxSize(effW, effH, allowMargins),
                                        )
                                    cur = CropSelectionMath.clamp(cur.copy(size = targetSize), effW, effH, allowMargins)
                                }
                                if (pan != Offset.Zero) {
                                    cur = CropSelectionMath.move(cur, pan.x / w, pan.y / h, effW, effH, allowMargins)
                                }
                                selection = cur
                            }
                        },
            ) {
                val stageW = stageSize.width.toFloat()
                val stageH = stageSize.height.toFloat()

                if (stageW > 0f && stageH > 0f) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val drawnW = effW * stageW
                        val drawnH = effH * stageH
                        drawImage(
                            image = bitmap,
                            dstOffset =
                                IntOffset(
                                    ((stageW - drawnW) / 2f).roundToInt(),
                                    ((stageH - drawnH) / 2f).roundToInt(),
                                ),
                            dstSize = IntSize(drawnW.roundToInt(), drawnH.roundToInt()),
                        )

                        val boxLeft = selection.left * stageW
                        val boxTop = selection.top * stageH
                        val boxW = selection.size * stageW
                        val boxH = selection.size * stageH

                        val scrim = Color.Black.copy(alpha = CROP_SCRIM_ALPHA)
                        drawRect(scrim, Offset(0f, 0f), Size(stageW, boxTop.coerceAtLeast(0f)))
                        drawRect(scrim, Offset(0f, boxTop + boxH), Size(stageW, (stageH - boxTop - boxH).coerceAtLeast(0f)))
                        drawRect(scrim, Offset(0f, boxTop), Size(boxLeft.coerceAtLeast(0f), boxH))
                        drawRect(
                            scrim,
                            Offset(boxLeft + boxW, boxTop),
                            Size((stageW - boxLeft - boxW).coerceAtLeast(0f), boxH),
                        )

                        drawRect(
                            color = colors.accent,
                            topLeft = Offset(boxLeft, boxTop),
                            size = Size(boxW, boxH),
                            style = Stroke(width = CROP_SELECTION_BORDER.toPx()),
                        )
                    }

                    val touchPx = with(density) { CROP_HANDLE_TOUCH_SIZE.toPx() }
                    val gripPx = with(density) { CROP_HANDLE_INDICATOR_SIZE.toPx() }
                    val boxLeft = selection.left * stageW
                    val boxTop = selection.top * stageH
                    val boxRight = boxLeft + selection.size * stageW
                    val boxBottom = boxTop + selection.size * stageH

                    CropCorner.entries.forEach { corner ->
                        val cornerX = if (corner.isLeft) boxLeft + gripPx / 2f else boxRight - gripPx / 2f
                        val cornerY = if (corner.isTop) boxTop + gripPx / 2f else boxBottom - gripPx / 2f
                        DragResizeHandle(
                            offset =
                                IntOffset(
                                    (cornerX - touchPx / 2f).roundToInt(),
                                    (cornerY - touchPx / 2f).roundToInt(),
                                ),
                            touchWidth = CROP_HANDLE_TOUCH_SIZE,
                            touchHeight = CROP_HANDLE_TOUCH_SIZE,
                            indicatorSize = CROP_HANDLE_INDICATOR_SIZE,
                            indicatorCorner = CROP_HANDLE_CORNER,
                            color = colors.accent,
                            onDragStart = { dragOrigin = selection },
                            onDrag = { totalX, totalY ->
                                selection =
                                    CropSelectionMath.resize(
                                        dragOrigin,
                                        corner,
                                        totalX / stageW,
                                        totalY / stageH,
                                        effW,
                                        effH,
                                        allowMargins,
                                    )
                            },
                        )
                    }
                }
            }
        }
    }

    if (showFitToggle) {
        GamepadChoiceCard(
            title = stringResource(R.string.layout_settings_bg_scale_mode),
            description = stringResource(R.string.button_settings_crop_scale_mode_desc),
            selectedText =
                when (fitMode) {
                    CropFitMode.FILL -> stringResource(R.string.bg_scale_mode_fill)
                    CropFitMode.FIT -> stringResource(R.string.bg_scale_mode_fit)
                    CropFitMode.STRETCH -> stringResource(R.string.bg_scale_mode_stretch)
                },
            icon = Icons.Rounded.AspectRatio,
            onPrevious = {
                fitMode =
                    when (fitMode) {
                        CropFitMode.FILL -> CropFitMode.STRETCH
                        CropFitMode.FIT -> CropFitMode.FILL
                        CropFitMode.STRETCH -> CropFitMode.FIT
                    }
                selection =
                    when (fitMode) {
                        CropFitMode.FILL -> CropSelectionMath.maxSelection(widthFraction, heightFraction, false)
                        CropFitMode.FIT -> CropSelectionMath.maxSelection(widthFraction, heightFraction, true)
                        CropFitMode.STRETCH -> CropSelectionMath.maxSelection(1f, 1f, false)
                    }
            },
            onNext = {
                fitMode =
                    when (fitMode) {
                        CropFitMode.FILL -> CropFitMode.FIT
                        CropFitMode.FIT -> CropFitMode.STRETCH
                        CropFitMode.STRETCH -> CropFitMode.FILL
                    }
                selection =
                    when (fitMode) {
                        CropFitMode.FILL -> CropSelectionMath.maxSelection(widthFraction, heightFraction, false)
                        CropFitMode.FIT -> CropSelectionMath.maxSelection(widthFraction, heightFraction, true)
                        CropFitMode.STRETCH -> CropSelectionMath.maxSelection(1f, 1f, false)
                    }
            },
            modifier = Modifier.firstDeckItem(),
        )
    }

    GamepadActionCard(
        title = stringResource(R.string.macropad_crop_apply),
        description = stringResource(R.string.macropad_crop_apply_desc),
        icon = Icons.Rounded.Check,
        onClick = {
            val baked =
                runCatching {
                    renderCroppedBitmap(
                        source = bitmap.asAndroidBitmap(),
                        aspectRatio = aspectRatio,
                        selection = selection,
                        widthFraction = widthFraction,
                        heightFraction = heightFraction,
                        fitMode = fitMode,
                    )
                }.onFailure { AppLog.e(TAG, "Crop source is not an Android bitmap", it) }
                    .getOrNull()
            if (baked != null) {
                onConfirm(baked)
            } else {
                AppLog.w(TAG, "Crop produced no bitmap")
                onCancel()
            }
        },
    )

    GamepadActionCard(
        title = stringResource(R.string.macropad_editor_cancel),
        description = stringResource(R.string.macropad_crop_cancel_desc),
        icon = Icons.AutoMirrored.Rounded.ArrowBack,
        onClick = onCancel,
    )
}
