package com.stormpanda.megingiard.macropad

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.stormpanda.megingiard.ui.LocalAppColors
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
    FIT,
    FILL,
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
        val rect = CropSelectionMath.imageRectInOutput(selection, widthFraction, heightFraction)
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

    var selection by remember {
        mutableStateOf(
            if (initialTransform != null) {
                CropSelectionMath.fromTransform(initialTransform, widthFraction, heightFraction, allowMargins)
            } else {
                CropSelectionMath.maxSelection(widthFraction, heightFraction, allowMargins)
            },
        )
    }
    var stageSize by remember { mutableStateOf(IntSize.Zero) }
    var dragOrigin by remember { mutableStateOf(selection) }

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
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.macropad_editor_cancel),
                    color = colors.onSurfaceSecondary,
                    style = MaterialTheme.typography.bodyMedium,
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
                    val transform = CropSelectionMath.toTransform(selection, widthFraction, heightFraction)
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
                                )
                            }.onFailure { AppLog.e(TAG, "Crop source is not an Android bitmap", it) }
                                .getOrNull()
                        if (baked != null) onConfirmBitmap(baked) else AppLog.w(TAG, "Crop produced no bitmap")
                    }
                },
            ) {
                Text(
                    text = stringResource(R.string.macropad_editor_done),
                    color = colors.accent,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
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
                    .onSizeChanged { stageSize = it }
                    .pointerInput(bitmap, widthFraction, heightFraction, allowMargins) {
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
                                        widthFraction,
                                        heightFraction,
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
                    val drawnW = widthFraction * stageW
                    val drawnH = heightFraction * stageH
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
                                    widthFraction,
                                    heightFraction,
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
                    label = stringResource(R.string.crop_image_fit_whole),
                    selected = fitMode == CropFitMode.FIT,
                    onClick = {
                        if (fitMode != CropFitMode.FIT) {
                            fitMode = CropFitMode.FIT
                            selection = CropSelectionMath.maxSelection(widthFraction, heightFraction, true)
                        }
                    },
                )
                CropFitModeChip(
                    label = stringResource(R.string.crop_image_fit_fill),
                    selected = fitMode == CropFitMode.FILL,
                    onClick = {
                        if (fitMode != CropFitMode.FILL) {
                            fitMode = CropFitMode.FILL
                            selection = CropSelectionMath.maxSelection(widthFraction, heightFraction, false)
                        }
                    },
                )
            }
        }

        Text(
            text = stringResource(R.string.layout_settings_crop_image_instructions),
            color = colors.onSurfaceSecondary,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = CROP_SPACING_12),
        )
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
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    Button(
        onClick = onClick,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = if (selected) colors.accent else colors.surfaceVariant,
                contentColor = if (selected) colors.onAccent else colors.onSurface,
            ),
        contentPadding = PaddingValues(horizontal = CROP_SPACING_16, vertical = CROP_SPACING_4),
        modifier = Modifier.height(CROP_CHIP_HEIGHT),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}
