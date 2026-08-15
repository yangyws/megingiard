package com.stormpanda.megingiard.macropad

import android.content.Context
import android.graphics.BitmapFactory
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.stormpanda.megingiard.AppLog
import com.stormpanda.megingiard.BitmapUtils
import com.stormpanda.megingiard.R
import com.stormpanda.megingiard.ui.LocalAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val TAG = "PadCanvas"

// ─────────────────────────────────────────────────────────────────────────────
// Constants
// ─────────────────────────────────────────────────────────────────────────────

private val ED_BUTTON_UNIT_DP = 60.dp
private val ED_BTN_SQUARE_RADIUS = 4.dp

// Reuse the shared screen padding so the editor canvas remains pixel-identical to use mode.
private val PC_SCREEN_PADDING = MP_SCREEN_PADDING
private const val ED_EDGE_MARGIN = 0.05f

// Grid: half a button unit — two steps apart = buttons touch exactly
private val PC_GRID_STEP_DP = 30.dp
private const val PC_GRID_LINE_ALPHA = 0.35f
private const val PC_GRID_STROKE_PX = 1f
private const val PC_RADIAL_CENTER_X = 0.5f
private const val PC_RADIAL_CENTER_Y = 0.5f

// Radial grid: snap points evenly distributed along each circle
private val PC_RADIAL_DOT_RADIUS = 3.dp
private val PC_RADIAL_CENTER_DOT = 5.dp
private const val PC_RADIAL_MIN_POINTS = 4
private const val PC_RADIAL_EXTRA_RINGS = 3

// Outer gradient edge alpha for editor chip buttons (matches use-mode resting appearance)
private const val PC_BTN_GRADIENT_OUTER = 0.9f

private enum class PressEnd {
    LIFTED,
    SLID,
    VANISHED,
}

private suspend fun AwaitPointerEventScope.awaitPressEnd(
    pointerId: PointerId,
    origin: Offset,
    slopPx: Float?,
    timeoutMillis: Long,
    onSample: (Offset) -> Unit,
): PressEnd? =
    withTimeoutOrNull(timeoutMillis) {
        while (true) {
            val change =
                awaitPointerEvent().changes.firstOrNull { it.id == pointerId }
                    ?: return@withTimeoutOrNull PressEnd.VANISHED
            if (!change.pressed) return@withTimeoutOrNull PressEnd.LIFTED
            onSample(change.position)
            if (slopPx != null && (change.position - origin).getDistance() > slopPx) {
                return@withTimeoutOrNull PressEnd.SLID
            }
        }
        @Suppress("UNREACHABLE_CODE")
        PressEnd.VANISHED
    }

// ─────────────────────────────────────────────────────────────────────────────
// Grid mode
// ─────────────────────────────────────────────────────────────────────────────

internal enum class GridMode { OFF, RECTANGULAR, RADIAL }

// ─────────────────────────────────────────────────────────────────────────────
// Pad canvas — drag buttons to reposition
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun PadCanvas(
    profile: PadProfile?,
    layout: PadLayout?,
    accentColor: Color,
    gridMode: GridMode,
    isLocked: Boolean,
    isBackgroundHidden: Boolean = false,
    onCellTap: ((col: Int, row: Int) -> Unit)? = null,
    onCellMove: ((from: Pair<Int, Int>, to: Pair<Int, Int>) -> Unit)? = null,
    onCellMenu: ((PadButton) -> Unit)? = null,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var lastTouchedButtonId by remember { mutableStateOf<String?>(null) }
    val colors = LocalAppColors.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val windowManager = remember { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    val bounds = windowManager.currentWindowMetrics.bounds
    val padWidth = with(density) { bounds.width().toDp() } - PC_SCREEN_PADDING * 2
    val padHeight = with(density) { bounds.height().toDp() } - PC_SCREEN_PADDING * 2
    val gridStepPx = with(density) { PC_GRID_STEP_DP.toPx() }

    var bgBitmap by remember(layout?.backgroundImagePath, layout?.backgroundImageVersion) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(layout?.backgroundImagePath, layout?.backgroundImageVersion) {
        val path = layout?.backgroundImagePath
        if (path != null) {
            try {
                val decoded = MacroPadMediaRepository.loadScaledBitmap(context, path)
                bgBitmap = decoded?.asImageBitmap()
            } catch (e: Exception) {
                AppLog.e(TAG, "Failed to decode background image $path", e)
                bgBitmap = null
            }
        } else {
            bgBitmap = null
        }
    }

    val bgImageDimFilter =
        remember(layout?.backgroundImageDim) {
            val dim = layout?.backgroundImageDim ?: 0f
            if (dim > 0f) {
                val scale = 1f - dim
                ColorFilter.colorMatrix(
                    ColorMatrix(
                        floatArrayOf(
                            scale,
                            0f,
                            0f,
                            0f,
                            0f,
                            0f,
                            scale,
                            0f,
                            0f,
                            0f,
                            0f,
                            0f,
                            scale,
                            0f,
                            0f,
                            0f,
                            0f,
                            0f,
                            1f,
                            0f,
                        ),
                    ),
                )
            } else {
                null
            }
        }

    val padModifier =
        Modifier
            .width(padWidth)
            .height(padHeight)
            .border(1.dp, colors.macroPadAccentBorder, RoundedCornerShape(0.dp))
            .clip(RoundedCornerShape(0.dp))
            .background(Color.Black)
            .onSizeChanged { canvasSize = it }

    Box(modifier = padModifier) {
        if (bgBitmap != null && !isBackgroundHidden) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cw = size.width
                val ch = size.height
                val iw = bgBitmap!!.width.toFloat()
                val ih = bgBitmap!!.height.toFloat()
                if (cw > 0f && ch > 0f && iw > 0f && ih > 0f) {
                    val scale = layout?.bgImageScale ?: 1f
                    val ox = layout?.bgImageOffsetX ?: 0f
                    val oy = layout?.bgImageOffsetY ?: 0f

                    val isFill = layout?.bgImageFill == true
                    val scaleBase =
                        if (isFill) {
                            com.stormpanda.megingiard.math.ViewportMath
                                .calculateAspectFillScale(cw, ch, iw, ih)
                        } else {
                            com.stormpanda.megingiard.math.ViewportMath
                                .calculateAspectFitScale(cw, ch, iw, ih)
                        }
                    val ws = iw * scaleBase
                    val hs = ih * scaleBase

                    val maxTx = ((ws * scale - cw) / 2f).coerceAtLeast(0f)
                    val maxTy = ((hs * scale - ch) / 2f).coerceAtLeast(0f)
                    val clampedX = (ox * cw).coerceIn(-maxTx, maxTx)
                    val clampedY = (oy * ch).coerceIn(-maxTy, maxTy)

                    drawImage(
                        image = bgBitmap!!,
                        dstOffset =
                            IntOffset(
                                ((cw - ws * scale) / 2f + clampedX).toInt(),
                                ((ch - hs * scale) / 2f + clampedY).toInt(),
                            ),
                        dstSize =
                            IntSize(
                                (ws * scale).toInt(),
                                (hs * scale).toInt(),
                            ),
                        colorFilter = bgImageDimFilter,
                    )
                }
            }
        }

        if (layout?.isGridMode == true) {
            PadTableGrid(
                layout = layout,
                accentColor = accentColor,
                onCellTap = onCellTap,
                onCellMove = onCellMove,
                onCellMenu = onCellMenu,
            )
        }

        // Render each button as a draggable chip (free-placement mode only)
        (if (layout?.isGridMode == true) emptyList() else layout?.buttons ?: emptyList()).forEach { btn ->
            val targetLayoutId = layout?.id
            DraggableButton(
                btn = btn,
                layout = layout!!,
                canvasSize = canvasSize,
                accentColor = accentColor,
                enableKeyboard = profile?.enableKeyboard == true,
                enableGamepad = profile?.enableGamepad == true,
                enableMouse = profile?.enableMouse == true,
                enableTouch = profile?.enableTouch == true,
                gridMode = gridMode,
                gridStepPx = gridStepPx,
                isLocked = isLocked,
                onTouch = { lastTouchedButtonId = btn.id },
                onPositionChanged = { nx, ny ->
                    val layoutId = targetLayoutId
                    val activeProfile = MacroPadState.activeProfile.value
                    if (layoutId != null && activeProfile != null) {
                        val currentLayout = activeProfile.layouts.firstOrNull { it.id == layoutId }
                        if (currentLayout != null) {
                            MacroPadState.updateLayout(
                                currentLayout.copy(
                                    buttons =
                                        currentLayout.buttons.map { b ->
                                            if (b.id == btn.id) b.copy(posX = nx, posY = ny) else b
                                        },
                                ),
                            )
                        }
                    }
                },
            )
        }

        // Render handles for the last touched button if not locked
        val activeBtn = (layout?.buttons ?: emptyList()).firstOrNull { it.id == lastTouchedButtonId }
        if (!isLocked && activeBtn != null) {
            val isTrackpoint = activeBtn.action is PadAction.TrackpointMove
            val tpMultiplier = if (isTrackpoint) (activeBtn.action as PadAction.TrackpointMove).size.multiplier else 1f
            val chipWidthPx =
                with(density) {
                    if (isTrackpoint) {
                        (ED_BUTTON_UNIT_DP * tpMultiplier).toPx()
                    } else {
                        (ED_BUTTON_UNIT_DP * activeBtn.buttonSize.cols).toPx()
                    }
                }
            val chipHeightPx =
                with(density) {
                    if (isTrackpoint) {
                        (ED_BUTTON_UNIT_DP * tpMultiplier).toPx()
                    } else {
                        (ED_BUTTON_UNIT_DP * activeBtn.buttonSize.rows).toPx()
                    }
                }

            val w = canvasSize.width.toFloat().coerceAtLeast(1f)
            val h = canvasSize.height.toFloat().coerceAtLeast(1f)

            val centerX = activeBtn.posX * w
            val centerY = activeBtn.posY * h

            val halfW = chipWidthPx / 2f
            val halfH = chipHeightPx / 2f

            val handleSize = 16.dp
            val handleSizePx = with(density) { handleSize.toPx() }
            val paddingPx = with(density) { 4.dp.toPx() }

            // Top handle
            DragHandle(
                buttonId = activeBtn.id,
                leftPx = centerX - handleSizePx / 2f,
                topPx = centerY - halfH - paddingPx - handleSizePx,
                handleSize = handleSize,
                buttonPosX = activeBtn.posX,
                buttonPosY = activeBtn.posY,
                w = w,
                h = h,
                gridMode = gridMode,
                gridStepPx = gridStepPx,
                layoutId = layout?.id,
                accentColor = accentColor,
            )

            // Bottom handle
            DragHandle(
                buttonId = activeBtn.id,
                leftPx = centerX - handleSizePx / 2f,
                topPx = centerY + halfH + paddingPx,
                handleSize = handleSize,
                buttonPosX = activeBtn.posX,
                buttonPosY = activeBtn.posY,
                w = w,
                h = h,
                gridMode = gridMode,
                gridStepPx = gridStepPx,
                layoutId = layout?.id,
                accentColor = accentColor,
            )

            // Left handle
            DragHandle(
                buttonId = activeBtn.id,
                leftPx = centerX - halfW - paddingPx - handleSizePx,
                topPx = centerY - handleSizePx / 2f,
                handleSize = handleSize,
                buttonPosX = activeBtn.posX,
                buttonPosY = activeBtn.posY,
                w = w,
                h = h,
                gridMode = gridMode,
                gridStepPx = gridStepPx,
                layoutId = layout?.id,
                accentColor = accentColor,
            )

            // Right handle
            DragHandle(
                buttonId = activeBtn.id,
                leftPx = centerX + halfW + paddingPx,
                topPx = centerY - handleSizePx / 2f,
                handleSize = handleSize,
                buttonPosX = activeBtn.posX,
                buttonPosY = activeBtn.posY,
                w = w,
                h = h,
                gridMode = gridMode,
                gridStepPx = gridStepPx,
                layoutId = layout?.id,
                accentColor = accentColor,
            )
        }

        // Grid overlay — drawn on topmost layer so it is never covered by buttons or backgrounds
        if (gridMode != GridMode.OFF && canvasSize.width > 0 && canvasSize.height > 0) {
            if (layout?.isGridMode == true) {
                TableGridOverlay(
                    layout = layout,
                    gridColor = accentColor.copy(alpha = PC_GRID_LINE_ALPHA),
                )
            } else {
                GridOverlay(
                    gridMode = gridMode,
                    gridStepPx = gridStepPx,
                    gridColor = accentColor.copy(alpha = PC_GRID_LINE_ALPHA),
                )
            }
        }
    }
}

@Composable
private fun DraggableButton(
    btn: PadButton,
    layout: PadLayout,
    canvasSize: IntSize,
    accentColor: Color,
    enableKeyboard: Boolean,
    enableGamepad: Boolean,
    enableMouse: Boolean,
    enableTouch: Boolean,
    gridMode: GridMode,
    gridStepPx: Float,
    isLocked: Boolean,
    onTouch: () -> Unit,
    onPositionChanged: (Float, Float) -> Unit,
) {
    val colors = LocalAppColors.current

    val resolvedBgColorOption = btn.buttonBgColor ?: layout.buttonBgColor
    val resolvedBorderColorOption = btn.buttonBorderColor ?: layout.buttonBorderColor
    val resolvedTextColorOption = btn.buttonTextColor ?: layout.buttonTextColor

    val effectiveBg = resolveColorOption(resolvedBgColorOption, accentColor, MP_AMBIENT_NEUTRAL_BG)
    val effectiveBorder = resolveColorOption(resolvedBorderColorOption, accentColor, MP_AMBIENT_NEUTRAL_BORDER)
    val effectiveTextTint = resolveColorOption(resolvedTextColorOption, accentColor, MP_AMBIENT_NEUTRAL_TEXT)

    // rememberUpdatedState lets the pointerInput closure (keyed only on btn.id +
    // canvasSize) see the live btn even though its lambda is NOT restarted when
    // btn.posX/posY change between drags.
    val currentBtn = rememberUpdatedState(btn)
    // Always call the latest onPositionChanged so PadCanvas's stale-profile
    // closure (captured by pointerInput) doesn't revert sibling button positions.
    val currentOnPositionChanged = rememberUpdatedState(onPositionChanged)
    val currentGridMode = rememberUpdatedState(gridMode)
    val currentGridStepPx = rememberUpdatedState(gridStepPx)
    // Anchor position captured at the moment the finger goes down.
    var startPosX by remember(btn.id) { mutableFloatStateOf(btn.posX) }
    var startPosY by remember(btn.id) { mutableFloatStateOf(btn.posY) }
    var dragOffsetX by remember(btn.id) { mutableFloatStateOf(0f) }
    var dragOffsetY by remember(btn.id) { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val isTrackpoint = btn.action is PadAction.TrackpointMove
    val isDeviceDisabled =
        when (val act = btn.action) {
            is PadAction.KeyboardKey -> {
                !enableKeyboard
            }

            is PadAction.GamepadButton -> {
                !enableGamepad
            }

            is PadAction.MouseButton,
            is PadAction.ScrollWheel,
            -> {
                !enableMouse
            }

            is PadAction.TrackpointMove -> {
                if (act.mode == TrackpointMode.VIRTUAL_TOUCH) !enableTouch else !enableMouse
            }

            is PadAction.Macro -> {
                !enableGamepad
            }

            is PadAction.BackgroundPeek -> {
                false
            }

            is PadAction.LayoutNext,
            is PadAction.LayoutPrevious,
            is PadAction.ProfileSwitcher,
            is PadAction.MirrorPlayStop,
            is PadAction.MirrorFreeze,
            is PadAction.MirrorViewportEdit,
            is PadAction.MirrorTouchProjection,
            -> {
                false
            }

            is PadAction.FullScreenMouse -> {
                !enableMouse
            }

            is PadAction.FullScreenKeyboard -> {
                !enableKeyboard
            }

            is PadAction.AppLauncher -> {
                false
            }
        }
    val tpMultiplier = if (isTrackpoint) (btn.action as PadAction.TrackpointMove).size.multiplier else 1f
    val chipWidthPx =
        with(density) {
            if (isTrackpoint) {
                (ED_BUTTON_UNIT_DP * tpMultiplier).toPx()
            } else {
                (ED_BUTTON_UNIT_DP * btn.buttonSize.cols).toPx()
            }
        }
    val chipHeightPx =
        with(density) {
            if (isTrackpoint) {
                (ED_BUTTON_UNIT_DP * tpMultiplier).toPx()
            } else {
                (ED_BUTTON_UNIT_DP * btn.buttonSize.rows).toPx()
            }
        }

    val w = canvasSize.width.toFloat().coerceAtLeast(1f)
    val h = canvasSize.height.toFloat().coerceAtLeast(1f)

    // Top-left position in canvas pixels (centre adjusted by half-chip)
    val left = btn.posX * w - chipWidthPx / 2f
    val top = btn.posY * h - chipHeightPx / 2f

    val isIconOnly = btn.buttonShape == ButtonShape.ICON_ONLY

    val btnWidthDp = if (isTrackpoint) ED_BUTTON_UNIT_DP * tpMultiplier else ED_BUTTON_UNIT_DP * btn.buttonSize.cols
    val btnHeightDp = if (isTrackpoint) ED_BUTTON_UNIT_DP * tpMultiplier else ED_BUTTON_UNIT_DP * btn.buttonSize.rows

    val chipShape =
        if (isTrackpoint) {
            CircleShape
        } else {
            when (btn.buttonShape) {
                ButtonShape.SQUARE, ButtonShape.ICON_ONLY -> {
                    RoundedCornerShape(ED_BTN_SQUARE_RADIUS)
                }

                ButtonShape.CIRCLE -> {
                    when (btn.buttonSize) {
                        ButtonSize.SIZE_2X2 -> CircleShape
                        ButtonSize.SIZE_2X1, ButtonSize.SIZE_1X2 -> RoundedCornerShape(percent = 50)
                        ButtonSize.SIZE_1X1 -> CircleShape
                    }
                }
            }
        }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .absoluteOffset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .width(btnWidthDp)
                .height(btnHeightDp)
                .then(
                    if (isLocked) {
                        Modifier
                    } else {
                        Modifier.pointerInput(btn.id, canvasSize) {
                            detectDragGestures(
                                onDragStart = {
                                    // Capture the current (live) position as anchor so the
                                    // accumulated delta is always relative to this drag's start.
                                    startPosX = currentBtn.value.posX
                                    startPosY = currentBtn.value.posY
                                    dragOffsetX = 0f
                                    dragOffsetY = 0f
                                    onTouch()
                                },
                                onDrag = { change, drag ->
                                    change.consume()
                                    dragOffsetX += drag.x
                                    dragOffsetY += drag.y
                                    val rawX = (startPosX + dragOffsetX / w).coerceIn(ED_EDGE_MARGIN, 1f - ED_EDGE_MARGIN)
                                    val rawY = (startPosY + dragOffsetY / h).coerceIn(ED_EDGE_MARGIN, 1f - ED_EDGE_MARGIN)
                                    val (snappedX, snappedY) =
                                        snapPosition(
                                            rawX,
                                            rawY,
                                            w,
                                            h,
                                            currentGridMode.value,
                                            currentGridStepPx.value,
                                        )
                                    currentOnPositionChanged.value(
                                        snappedX.coerceIn(ED_EDGE_MARGIN, 1f - ED_EDGE_MARGIN),
                                        snappedY.coerceIn(ED_EDGE_MARGIN, 1f - ED_EDGE_MARGIN),
                                    )
                                },
                            )
                        }
                    },
                ).then(
                    if (isLocked) {
                        Modifier
                    } else {
                        Modifier.pointerInput(btn.id) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val isTouchDown = event.changes.any { it.pressed && !it.previousPressed }
                                    if (isTouchDown) {
                                        onTouch()
                                    }
                                }
                            }
                        }
                    },
                ),
    ) {
        PadButtonFace(
            width = btnWidthDp,
            height = btnHeightDp,
            shape = chipShape,
            isIconOnly = isIconOnly,
            isDeviceDisabled = isDeviceDisabled,
            borderColor = effectiveBorder,
            bgColor = effectiveBg,
            bgAlpha = 0.25f,
            gradientScale = PC_BTN_GRADIENT_OUTER / 0.25f,
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(
                        if (btn.invisible) {
                            Modifier.graphicsLayer { alpha = 0.4f }
                        } else {
                            Modifier
                        },
                    ),
        ) {
            PadButtonContent(
                btn = btn,
                effectiveTextTint = effectiveTextTint,
                iconSize = MP_BUTTON_UNIT_DP * 0.73f * minOf(btn.buttonSize.cols, btn.buttonSize.rows),
                isTrackpoint = isTrackpoint,
            )
        }
        if (btn.invisible) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                contentAlignment = Alignment.TopEnd,
            ) {
                MaterialSymbol(
                    name = "visibility_off",
                    size = 14.dp,
                    tint = effectiveTextTint,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Grid overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TableGridOverlay(
    layout: PadLayout,
    gridColor: Color,
) {
    val cols = layout.effectiveGridCols
    val rows = layout.effectiveGridRows
    if (cols <= 0 || rows <= 0) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val cellW = w / cols
        val cellH = h / rows

        // Vertical division lines
        for (c in 1 until cols) {
            val x = c * cellW
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = PC_GRID_STROKE_PX,
            )
        }

        // Horizontal division lines
        for (r in 1 until rows) {
            val y = r * cellH
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = PC_GRID_STROKE_PX,
            )
        }
    }
}

@Composable
private fun GridOverlay(
    gridMode: GridMode,
    gridStepPx: Float,
    gridColor: Color,
) {
    val density = LocalDensity.current
    val dotRadiusPx = with(density) { PC_RADIAL_DOT_RADIUS.toPx() }
    val centerDotPx = with(density) { PC_RADIAL_CENTER_DOT.toPx() }
    val buttonUnitPx = with(density) { ED_BUTTON_UNIT_DP.toPx() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        when (gridMode) {
            GridMode.OFF -> { /* no-op */ }

            GridMode.RECTANGULAR -> {
                // Lines are centred on the canvas midpoint so the rectangular
                // grid shares its origin with the radial grid's circle centre.
                val cx = w * PC_RADIAL_CENTER_X
                val cy = h * PC_RADIAL_CENTER_Y
                // Vertical lines outward from centre
                var dx = 0f
                while (cx - dx >= 0f || cx + dx <= w) {
                    if (cx + dx <= w) {
                        drawLine(gridColor, Offset(cx + dx, 0f), Offset(cx + dx, h), strokeWidth = PC_GRID_STROKE_PX)
                    }
                    if (dx > 0f && cx - dx >= 0f) {
                        drawLine(gridColor, Offset(cx - dx, 0f), Offset(cx - dx, h), strokeWidth = PC_GRID_STROKE_PX)
                    }
                    dx += gridStepPx
                }
                // Horizontal lines outward from centre
                var dy = 0f
                while (cy - dy >= 0f || cy + dy <= h) {
                    if (cy + dy <= h) {
                        drawLine(gridColor, Offset(0f, cy + dy), Offset(w, cy + dy), strokeWidth = PC_GRID_STROKE_PX)
                    }
                    if (dy > 0f && cy - dy >= 0f) {
                        drawLine(gridColor, Offset(0f, cy - dy), Offset(w, cy - dy), strokeWidth = PC_GRID_STROKE_PX)
                    }
                    dy += gridStepPx
                }
            }

            GridMode.RADIAL -> {
                val cx = w * PC_RADIAL_CENTER_X
                val cy = h * PC_RADIAL_CENTER_Y
                val maxRadius = maxOf(w, h) / 2f
                val dotRadius = dotRadiusPx
                val centerDotRadius = centerDotPx
                val dotColor = gridColor

                // Concentric circles with evenly-distributed snap dots.
                // Odd circles (1, 3, 5 …): phase 45° → diagonals as anchors.
                // Even circles (2, 4, 6 …): phase 0° → cardinal directions as anchors.
                var r = gridStepPx
                var circleIndex = 1
                while (r <= maxRadius + PC_RADIAL_EXTRA_RINGS * gridStepPx) {
                    drawCircle(gridColor, radius = r, center = Offset(cx, cy), style = Stroke(PC_GRID_STROKE_PX))
                    val n = radialPointCount(r, buttonUnitPx)
                    val phaseOffset = if (circleIndex % 2 == 1) PI / 4.0 else 0.0
                    val angleStep = 2.0 * PI / n
                    for (i in 0 until n) {
                        val angle = (phaseOffset + i * angleStep).toFloat()
                        val px = cx + r * cos(angle)
                        val py = cy + r * sin(angle)
                        drawCircle(dotColor, radius = dotRadius, center = Offset(px, py))
                    }
                    r += gridStepPx
                    circleIndex++
                }

                // Center snap point
                drawCircle(dotColor, radius = centerDotRadius, center = Offset(cx, cy))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Snap helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Snap a normalised position to the active grid. Returns the (possibly unchanged)
 * normalised coordinates.
 */
private fun snapPosition(
    rawNormX: Float,
    rawNormY: Float,
    canvasW: Float,
    canvasH: Float,
    gridMode: GridMode,
    gridStepPx: Float,
): Pair<Float, Float> =
    when (gridMode) {
        GridMode.OFF -> rawNormX to rawNormY
        GridMode.RECTANGULAR -> snapRectangular(rawNormX, rawNormY, canvasW, canvasH, gridStepPx)
        GridMode.RADIAL -> snapRadial(rawNormX, rawNormY, canvasW, canvasH, gridStepPx)
    }

/**
 * Round to nearest grid intersection. The grid is centred on the canvas midpoint
 * (same origin as the radial circles) so the centre is always a cross-point.
 */
private fun snapRectangular(
    rawNormX: Float,
    rawNormY: Float,
    canvasW: Float,
    canvasH: Float,
    gridStepPx: Float,
): Pair<Float, Float> {
    val rawPxX = rawNormX * canvasW
    val rawPxY = rawNormY * canvasH
    val cx = canvasW * PC_RADIAL_CENTER_X
    val cy = canvasH * PC_RADIAL_CENTER_Y
    val snappedPxX = cx + ((rawPxX - cx) / gridStepPx).roundToInt() * gridStepPx
    val snappedPxY = cy + ((rawPxY - cy) / gridStepPx).roundToInt() * gridStepPx
    return (snappedPxX / canvasW) to (snappedPxY / canvasH)
}

/**
 * Snap to the nearest evenly-distributed point on a concentric circle, or to the
 * center point. Circles alternate phase:
 *   odd  (1, 3, 5 …) → 45° offset → diagonal anchors
 *   even (2, 4, 6 …) → 0° offset  → cardinal anchors
 */
private fun snapRadial(
    rawNormX: Float,
    rawNormY: Float,
    canvasW: Float,
    canvasH: Float,
    gridStepPx: Float,
): Pair<Float, Float> {
    val rawPxX = rawNormX * canvasW
    val rawPxY = rawNormY * canvasH
    val cx = canvasW * PC_RADIAL_CENTER_X
    val cy = canvasH * PC_RADIAL_CENTER_Y

    val dx = rawPxX - cx
    val dy = rawPxY - cy
    val rawRadius = sqrt(dx * dx + dy * dy)

    // Grid step is always half the button unit
    val buttonUnitPx = gridStepPx * 2f

    // Snap radius to nearest circle (or 0 = center)
    val snappedRadius = (round(rawRadius / gridStepPx) * gridStepPx)

    // Center snap
    if (snappedRadius < gridStepPx * 0.5f) {
        return (cx / canvasW) to (cy / canvasH)
    }

    // Determine phase offset for this circle
    val circleIndex = round(snappedRadius / gridStepPx).toInt()
    val phaseOffset = if (circleIndex % 2 == 1) PI / 4.0 else 0.0

    val n = radialPointCount(snappedRadius, buttonUnitPx)
    val angleStep = 2.0 * PI / n

    // Snap to nearest point: work in phase-relative angle space
    val rawAngle = atan2(dy.toDouble(), dx.toDouble()) // −π..π
    val relAngle = rawAngle - phaseOffset // shift to phase origin
    val relAnglePos = if (relAngle < 0) relAngle + 2 * PI else relAngle // 0..2π
    val nearestIndex = round(relAnglePos / angleStep).toInt() % n
    val snappedAngle = phaseOffset + nearestIndex * angleStep

    val snappedPxX = cx + snappedRadius * cos(snappedAngle).toFloat()
    val snappedPxY = cy + snappedRadius * sin(snappedAngle).toFloat()

    // Also consider the center point — pick whichever is closer
    val distToCircle = dist(rawPxX, rawPxY, snappedPxX, snappedPxY)
    val distToCenter = dist(rawPxX, rawPxY, cx, cy)
    return if (distToCenter < distToCircle) {
        (cx / canvasW) to (cy / canvasH)
    } else {
        (snappedPxX / canvasW) to (snappedPxY / canvasH)
    }
}

/** Euclidean distance between two points. */
private fun dist(
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
): Float {
    val dx = x1 - x2
    val dy = y1 - y2
    return sqrt(dx * dx + dy * dy)
}

/**
 * How many evenly-distributed snap points to place on a circle of the given radius.
 * Scales with circumference — roughly one point per [buttonUnitPx] of arc length.
 * Always rounded to the nearest multiple of 4 (minimum 4) so the 4 phase-anchor
 * points (cardinal or diagonal) land at exact positions.
 */
private fun radialPointCount(
    radiusPx: Float,
    buttonUnitPx: Float,
): Int {
    val circumference = (2.0 * PI * radiusPx).toFloat()
    val raw = round(circumference / buttonUnitPx).toInt().coerceAtLeast(1)
    // Round to nearest multiple of 4, minimum 4
    val rounded4 = ((raw + 2) / 4) * 4
    return maxOf(PC_RADIAL_MIN_POINTS, rounded4)
}

@Composable
private fun DragHandle(
    buttonId: String,
    leftPx: Float,
    topPx: Float,
    handleSize: Dp,
    buttonPosX: Float,
    buttonPosY: Float,
    w: Float,
    h: Float,
    gridMode: GridMode,
    gridStepPx: Float,
    layoutId: String?,
    accentColor: Color,
) {
    var startPosX by remember(buttonId) { mutableFloatStateOf(buttonPosX) }
    var startPosY by remember(buttonId) { mutableFloatStateOf(buttonPosY) }
    var dragOffsetX by remember(buttonId) { mutableFloatStateOf(0f) }
    var dragOffsetY by remember(buttonId) { mutableFloatStateOf(0f) }

    Box(
        modifier =
            Modifier
                .absoluteOffset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
                .size(handleSize)
                .pointerInput(buttonId, w, h) {
                    detectDragGestures(
                        onDragStart = {
                            startPosX = buttonPosX
                            startPosY = buttonPosY
                            dragOffsetX = 0f
                            dragOffsetY = 0f
                        },
                        onDrag = { change, drag ->
                            change.consume()
                            dragOffsetX += drag.x
                            dragOffsetY += drag.y
                            val rawX = (startPosX + dragOffsetX / w).coerceIn(ED_EDGE_MARGIN, 1f - ED_EDGE_MARGIN)
                            val rawY = (startPosY + dragOffsetY / h).coerceIn(ED_EDGE_MARGIN, 1f - ED_EDGE_MARGIN)
                            val (snappedX, snappedY) =
                                snapPosition(
                                    rawX,
                                    rawY,
                                    w,
                                    h,
                                    gridMode,
                                    gridStepPx,
                                )
                            val activeProfile = MacroPadState.activeProfile.value
                            if (layoutId != null && activeProfile != null) {
                                val currentLayout = activeProfile.layouts.firstOrNull { it.id == layoutId }
                                if (currentLayout != null) {
                                    MacroPadState.updateLayout(
                                        currentLayout.copy(
                                            buttons =
                                                currentLayout.buttons.map { b ->
                                                    if (b.id == buttonId) {
                                                        b.copy(
                                                            posX = snappedX.coerceIn(ED_EDGE_MARGIN, 1f - ED_EDGE_MARGIN),
                                                            posY = snappedY.coerceIn(ED_EDGE_MARGIN, 1f - ED_EDGE_MARGIN),
                                                        )
                                                    } else {
                                                        b
                                                    }
                                                },
                                        ),
                                    )
                                }
                            }
                        },
                    )
                },
    ) {
        MaterialSymbol(
            name = "drag_pan",
            size = handleSize,
            tint = accentColor,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Table mode canvas
// ─────────────────────────────────────────────────────────────────────────────

private val PC_TABLE_ICON_SIZE = 24.dp
private val PC_TABLE_ADD_ICON_SIZE = 20.dp
private val PC_TABLE_CONTENT_PADDING = 2.dp
private const val PC_TABLE_FILL_ALPHA = 0.18f
private const val PC_TABLE_EMPTY_ICON_ALPHA = 0.5f
private val PC_TABLE_DROP_BORDER_WIDTH = 2.5.dp
private const val PC_TABLE_DRAG_SOURCE_ALPHA = 0.4f
private val PC_TABLE_SELECTED_BORDER = Color(0xFFE53935)
private val PC_TABLE_SELECTED_BG = Color(0x40E53935)
private val MP_TABLE_CELL_CORNER_RADIUS = 4.dp
private val MP_TABLE_GRID_LINE_WIDTH = 1.dp
private val MP_TABLE_GRID_LINE_COLOR = Color(0x33FFFFFF)

@Composable
private fun PadTableGrid(
    layout: PadLayout,
    accentColor: Color,
    onCellTap: ((col: Int, row: Int) -> Unit)? = null,
    onCellMove: ((from: Pair<Int, Int>, to: Pair<Int, Int>) -> Unit)? = null,
    onCellMenu: ((PadButton) -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val cols = layout.effectiveGridCols
    val rows = layout.effectiveGridRows
    var gridSize by remember { mutableStateOf(IntSize.Zero) }

    var pressCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var pressPhase by remember { mutableStateOf(TableCellPressPhase.TAP) }
    var dragOver by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var dragMoved by remember { mutableStateOf(false) }

    val layoutRef by rememberUpdatedState(layout)
    val onCellTapRef by rememberUpdatedState(onCellTap)
    val onCellMoveRef by rememberUpdatedState(onCellMove)

    fun cellOf(position: Offset): Pair<Int, Int>? {
        if (gridSize.width <= 0 || gridSize.height <= 0) return null
        return GridLayoutMath.cellAt(
            position.x / gridSize.width,
            position.y / gridSize.height,
            cols,
            rows,
        )
    }

    fun resetTablePress() {
        pressCell = null
        pressPhase = TableCellPressPhase.TAP
        dragOver = null
        dragMoved = false
    }

    val cellShapes =
        remember(layout, gridSize, cols, rows, density) {
            val requestedRadiusPx = with(density) { MP_TABLE_CELL_CORNER_RADIUS.toPx() }
            List(rows) { row ->
                List(cols) { col ->
                    val radii =
                        GridLayoutMath.cellFaceRadiiPx(
                            layout = layout,
                            col = col,
                            row = row,
                            outlineEmptyCells = true,
                            faceWidthPx = if (cols > 0) gridSize.width.toFloat() / cols else 0f,
                            faceHeightPx = if (rows > 0) gridSize.height.toFloat() / rows else 0f,
                            requestedRadiusPx = requestedRadiusPx,
                        )
                    with(density) {
                        RoundedCornerShape(
                            topStart = radii.topLeftPx.toDp(),
                            topEnd = radii.topRightPx.toDp(),
                            bottomEnd = radii.bottomRightPx.toDp(),
                            bottomStart = radii.bottomLeftPx.toDp(),
                        )
                    }
                }
            }
        }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .then(
                    if (layout.gridShowBorders) {
                        Modifier.padding(PTC_TABLE_THICK_BORDER_OUTER_PADDING)
                    } else {
                        Modifier
                    },
                )
                .onSizeChanged { gridSize = it }
                .pointerInput(layout.id, cols, rows) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        try {
                            val startCell = cellOf(down.position)
                            val slop = viewConfiguration.touchSlop
                            val startHasButton =
                                startCell != null &&
                                    GridLayoutMath.buttonAt(layoutRef, startCell.first, startCell.second) != null
                            var lastPosition = down.position

                            val tapEnd =
                                awaitPressEnd(
                                    pointerId = down.id,
                                    origin = down.position,
                                    slopPx = slop,
                                    timeoutMillis = viewConfiguration.longPressTimeoutMillis,
                                    onSample = { lastPosition = it },
                                )
                            if (tapEnd != null) {
                                val outcome =
                                    TableCellPressRules.outcomeOnRelease(
                                        phase = TableCellPressPhase.TAP,
                                        slidBeforeLongPress = tapEnd != PressEnd.LIFTED,
                                        movedAfterPickUp = false,
                                        cellHasButton = startHasButton,
                                    )
                                if (outcome == TableCellPressOutcome.TAP_CELL) {
                                    startCell?.let { (col, row) -> onCellTapRef?.invoke(col, row) }
                                }
                                return@awaitEachGesture
                            }

                            val held = startCell?.takeIf { startHasButton } ?: return@awaitEachGesture

                            pressCell = held
                            pressPhase = TableCellPressPhase.MOVE
                            dragOver = held
                            dragMoved = false
                            val dragOrigin = lastPosition
                            var lifted = false
                            while (true) {
                                val change =
                                    awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    lifted = true
                                    break
                                }
                                if (!dragMoved && (change.position - dragOrigin).getDistance() > slop) {
                                    dragMoved = true
                                }
                                if (TableCellPressRules.tracksDragTarget(TableCellPressPhase.MOVE, dragMoved)) {
                                    dragOver = cellOf(change.position)
                                    change.consume()
                                }
                            }

                            val outcome =
                                if (lifted) {
                                    TableCellPressRules.outcomeOnRelease(
                                        phase = TableCellPressPhase.MOVE,
                                        slidBeforeLongPress = false,
                                        movedAfterPickUp = dragMoved,
                                        cellHasButton = true,
                                    )
                                } else {
                                    TableCellPressOutcome.IGNORE
                                }
                            if (outcome == TableCellPressOutcome.MOVE_CELL) {
                                val target = dragOver
                                if (target != null && target != held) {
                                    onCellMoveRef?.invoke(held, target)
                                }
                            }
                        } finally {
                            resetTablePress()
                        }
                    }
                },
    ) {
        Layout(
            content = {
                val uniqueButtons =
                    remember(layout, cols, rows) {
                        layout.buttons.filter { it.isWithinGrid(cols, rows) }
                    }
                val pCell = pressCell
                val dOver = dragOver
                val sourceButton = if (pCell != null) GridLayoutMath.buttonAt(layout, pCell.first, pCell.second) else null
                val isDraggingTarget =
                    TableCellPressRules.tracksDragTarget(pressPhase, dragMoved) &&
                        pCell != null &&
                        dOver != null &&
                        sourceButton != null &&
                        dOver != pCell

                val cellWPx = if (cols > 0) gridSize.width.toFloat() / cols else 0f
                val cellHPx = if (rows > 0) gridSize.height.toFloat() / rows else 0f

                uniqueButtons.forEach { button ->
                    val col = button.gridCol!!
                    val row = button.gridRow!!
                    val cellShape =
                        if (row in 0 until rows && col in 0 until cols) cellShapes[row][col] else RoundedCornerShape(4.dp)
                    val isPressedButton = sourceButton?.id == button.id
                    val isPickedUp = isPressedButton && TableCellPressRules.showsMoveArmedFrame(pressPhase)
                    val isSource = isPickedUp && dragMoved

                    val widthDp = with(density) { (cellWPx * button.effectiveColSpan).toDp() }
                    val heightDp = with(density) { (cellHPx * button.effectiveRowSpan).toDp() }
                    val faceSize =
                        if (cellWPx > 0f && cellHPx > 0f) {
                            minOf(widthDp, heightDp)
                        } else {
                            48.dp * minOf(button.effectiveColSpan, button.effectiveRowSpan)
                        }

                    PadTableCell(
                        button = button,
                        layout = layout,
                        accentColor = accentColor,
                        shape = cellShape,
                        faceSize = faceSize,
                        isPickedUp = isPickedUp,
                        isDragSource = isSource,
                        isDropTarget = false,
                        modifier = Modifier,
                    )
                }

                val emptyCells =
                    remember(layout, cols, rows) {
                        val cells = mutableListOf<Pair<Int, Int>>()
                        for (r in 0 until rows) {
                            for (c in 0 until cols) {
                                if (GridLayoutMath.buttonAt(layout, c, r) == null) {
                                    cells.add(c to r)
                                }
                            }
                        }
                        cells
                    }
                emptyCells.forEach { (col, row) ->
                    val cellShape =
                        if (row in 0 until rows && col in 0 until cols) cellShapes[row][col] else RoundedCornerShape(4.dp)

                    Box(
                        modifier =
                            Modifier
                                .then(
                                    if (layout.gridShowBorders) {
                                        Modifier
                                            .padding(PTC_TABLE_THICK_BORDER_CELL_PADDING)
                                            .border(
                                                width = PTC_TABLE_THICK_BORDER_WIDTH,
                                                color = PTC_TABLE_THICK_BORDER_COLOR,
                                                shape = cellShape,
                                            )
                                    } else {
                                        Modifier
                                    },
                                )
                                .then(
                                    if (layout.gridShowButtonBg) {
                                        Modifier.background(PTC_TABLE_BASE_BG, cellShape)
                                    } else {
                                        Modifier.background(PTC_TABLE_BASE_BG.copy(alpha = 0.2f), cellShape)
                                    },
                                )
                                .clip(cellShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        MaterialSymbol(
                            name = "add",
                            size = PC_TABLE_ADD_ICON_SIZE,
                            tint = colors.onSurfaceSecondary.copy(alpha = PC_TABLE_EMPTY_ICON_ALPHA),
                            filled = false,
                        )
                    }
                }

                if (isDraggingTarget && sourceButton != null && dOver != null && pCell != null) {
                    val sCol = sourceButton.gridCol!!
                    val sRow = sourceButton.gridRow!!
                    val touchOffsetCol = pCell.first - sCol
                    val touchOffsetRow = pCell.second - sRow
                    val tCol = dOver.first - touchOffsetCol
                    val tRow = dOver.second - touchOffsetRow

                    val isValidTarget = GridLayoutMath.canMoveButton(layout, pCell, dOver)
                    val targetShape = cellShapes[tRow.coerceIn(0, rows - 1)][tCol.coerceIn(0, cols - 1)]

                    Box(
                        modifier =
                            Modifier
                                .background(
                                    color = if (isValidTarget) Color.Transparent else colors.error.copy(alpha = 0.22f),
                                    shape = targetShape,
                                )
                                .border(
                                    width = PC_TABLE_DROP_BORDER_WIDTH,
                                    color = if (isValidTarget) PC_TABLE_SELECTED_BORDER else colors.error,
                                    shape = targetShape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!isValidTarget) {
                            MaterialSymbol(
                                name = "block",
                                size = 32.dp,
                                tint = colors.error,
                                filled = false,
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { measurables, constraints ->
            val totalW = constraints.maxWidth
            val totalH = constraints.maxHeight
            val cellW = totalW.toFloat() / cols
            val cellH = totalH.toFloat() / rows

            val uniqueButtons = layout.buttons.filter { it.isWithinGrid(cols, rows) }
            val emptyCells = mutableListOf<Pair<Int, Int>>()
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    if (GridLayoutMath.buttonAt(layout, c, r) == null) {
                        emptyCells.add(c to r)
                    }
                }
            }

            val pCell = pressCell
            val dOver = dragOver
            val sourceButton = if (pCell != null) GridLayoutMath.buttonAt(layout, pCell.first, pCell.second) else null
            val isDraggingTarget =
                TableCellPressRules.tracksDragTarget(pressPhase, dragMoved) &&
                    pCell != null &&
                    dOver != null &&
                    sourceButton != null

            var idx = 0
            val placeables = mutableListOf<Triple<androidx.compose.ui.layout.Placeable, Int, Int>>()

            uniqueButtons.forEach { button ->
                val col = button.gridCol!!
                val row = button.gridRow!!
                val cs = button.effectiveColSpan
                val rs = button.effectiveRowSpan

                val targetW = (cellW * cs).roundToInt().coerceAtLeast(0)
                val targetH = (cellH * rs).roundToInt().coerceAtLeast(0)

                if (idx < measurables.size) {
                    val placeable = measurables[idx++].measure(Constraints.fixed(targetW, targetH))
                    val x = (cellW * col).roundToInt()
                    val y = (cellH * row).roundToInt()
                    placeables.add(Triple(placeable, x, y))
                }
            }

            emptyCells.forEach { (col, row) ->
                val targetW = cellW.roundToInt().coerceAtLeast(0)
                val targetH = cellH.roundToInt().coerceAtLeast(0)

                if (idx < measurables.size) {
                    val placeable = measurables[idx++].measure(Constraints.fixed(targetW, targetH))
                    val x = (cellW * col).roundToInt()
                    val y = (cellH * row).roundToInt()
                    placeables.add(Triple(placeable, x, y))
                }
            }

            if (isDraggingTarget && sourceButton != null && dOver != null && pCell != null && idx < measurables.size) {
                val sCol = sourceButton.gridCol!!
                val sRow = sourceButton.gridRow!!
                val touchOffsetCol = pCell.first - sCol
                val touchOffsetRow = pCell.second - sRow
                val tCol = dOver.first - touchOffsetCol
                val tRow = dOver.second - touchOffsetRow
                val tColSpan = sourceButton.effectiveColSpan
                val tRowSpan = sourceButton.effectiveRowSpan

                val targetW = (cellW * tColSpan).roundToInt().coerceAtLeast(0)
                val targetH = (cellH * tRowSpan).roundToInt().coerceAtLeast(0)

                val placeable = measurables[idx++].measure(Constraints.fixed(targetW, targetH))
                val x = (cellW * tCol).roundToInt()
                val y = (cellH * tRow).roundToInt()
                placeables.add(Triple(placeable, x, y))
            }

            layout(totalW, totalH) {
                placeables.forEach { (placeable, x, y) ->
                    placeable.placeRelative(x, y)
                }
            }
        }

        PadTableGridLines(layout = layout)
    }
}
