package com.stormpanda.megingiard.macropad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

private const val TAG = "PadTableComponents"

internal val PTC_TABLE_ICON_SIZE = 24.dp
internal val PTC_TABLE_ADD_ICON_SIZE = 20.dp
internal val PTC_TABLE_CONTENT_PADDING = 2.dp
internal const val PTC_TABLE_FILL_ALPHA_IDLE = 0.12f
internal const val PTC_TABLE_FILL_ALPHA_PRESSED = 0.38f
internal const val PTC_TABLE_EMPTY_ICON_ALPHA = 0.5f
internal val PTC_TABLE_DROP_BORDER_WIDTH = 2.5.dp
internal const val PTC_TABLE_DRAG_SOURCE_ALPHA = 0.4f
internal val PTC_TABLE_SELECTED_BORDER = Color(0xFFE53935)
internal val PTC_TABLE_SELECTED_BG = Color(0x40E53935)
internal val PTC_TABLE_CELL_CORNER_RADIUS = 4.dp
internal val PTC_TABLE_GRID_LINE_WIDTH = 1.dp
internal val PTC_TABLE_GRID_LINE_COLOR = Color(0x33FFFFFF)
internal val PTC_TABLE_LABEL_RESERVE = 16.dp

@Composable
internal fun PadTableCell(
    button: PadButton,
    layout: PadLayout,
    accentColor: Color,
    shape: Shape,
    faceSize: Dp = 48.dp,
    isPressed: Boolean = false,
    isRunning: Boolean = false,
    isDeviceDisabled: Boolean = false,
    isPickedUp: Boolean = false,
    isDragSource: Boolean = false,
    isDropTarget: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val bg = resolveColorOption(button.buttonBgColor ?: layout.buttonBgColor, accentColor, MP_AMBIENT_NEUTRAL_BG)
    val text =
        resolveColorOption(button.buttonTextColor ?: layout.buttonTextColor, accentColor, MP_AMBIENT_NEUTRAL_TEXT)

    Box(
        modifier =
            modifier
                .graphicsLayer {
                    alpha =
                        when {
                            isDragSource -> PTC_TABLE_DRAG_SOURCE_ALPHA
                            isDeviceDisabled -> 0.4f
                            else -> 1f
                        }
                }
                .clip(shape)
                .drawBehind {
                    val alpha = if (isPressed) PTC_TABLE_FILL_ALPHA_PRESSED else PTC_TABLE_FILL_ALPHA_IDLE
                    val effectiveBg =
                        if (isPickedUp) PTC_TABLE_SELECTED_BG else bg.copy(alpha = (bg.alpha * alpha).coerceIn(0f, 1f))
                    drawRect(effectiveBg)
                }
                .then(
                    if (isPickedUp || isDropTarget) {
                        Modifier.border(
                            width = PTC_TABLE_DROP_BORDER_WIDTH,
                            color = PTC_TABLE_SELECTED_BORDER,
                            shape = shape,
                        )
                    } else {
                        Modifier
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        PadButtonContent(
            btn = button,
            effectiveTextTint = text,
            iconSize = PTC_TABLE_ICON_SIZE,
            faceSize = faceSize,
            isTrackpoint = false,
            effectiveContentAccent = accentColor,
            isPressed = isPressed,
        )
    }
}

@Composable
internal fun PadTableGridLines(layout: PadLayout) {
    val density = LocalDensity.current
    val strokePx = with(density) { PTC_TABLE_GRID_LINE_WIDTH.toPx() }
    val lines = remember(layout) { GridLayoutMath.gridLines(layout, outlineEmptyCells = true) }
    if (lines.isEmpty()) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        lines.forEach { line ->
            val r = GridLayoutMath.gridLineRect(line, size.width, size.height, strokePx)
            drawRect(
                color = PTC_TABLE_GRID_LINE_COLOR,
                topLeft = Offset(r.left, r.top),
                size = Size(r.width, r.height),
            )
        }
    }
}

@Composable
internal fun PadLiveTableGrid(
    profile: PadProfile,
    layout: PadLayout,
    canvasSize: IntSize,
    accentColor: Color,
    pressedIds: Set<String>,
    runningMacroIds: Set<String>,
    isPeekActive: Boolean,
) {
    val density = LocalDensity.current
    val cols = layout.effectiveGridCols
    val rows = layout.effectiveGridRows
    val w = canvasSize.width.toFloat()
    val h = canvasSize.height.toFloat()

    if (w <= 0f || h <= 0f) return

    val cellW = w / cols
    val cellH = h / rows

    val visibleButtons =
        remember(layout, cols, rows, isPeekActive) {
            val base = layout.buttons.filter { it.isWithinGrid(cols, rows) }
            if (isPeekActive) base.filter { it.action is PadAction.BackgroundPeek } else base
        }

    Box(modifier = Modifier.fillMaxSize()) {
        visibleButtons.forEach { button ->
            key(button.id) {
                val col = button.gridCol ?: 0
                val row = button.gridRow ?: 0
                val cs = button.effectiveColSpan
                val rs = button.effectiveRowSpan
                val left = (cellW * col).roundToInt()
                val top = (cellH * row).roundToInt()
                val widthDp = with(density) { (cellW * cs).toDp() }
                val heightDp = with(density) { (cellH * rs).toDp() }
                val faceSize = minOf(widthDp, heightDp)
                val isPressed = button.id in pressedIds
                val isRunning =
                    button.action is PadAction.Macro &&
                        (button.action as PadAction.Macro).macroId in runningMacroIds
                val isDeviceDisabled = MacroPadHitTestEngine.isDeviceDisabled(button.action, profile)

                PadTableCell(
                    button = button,
                    layout = layout,
                    accentColor = accentColor,
                    shape = RoundedCornerShape(PTC_TABLE_CELL_CORNER_RADIUS),
                    faceSize = faceSize,
                    isPressed = isPressed,
                    isRunning = isRunning,
                    isDeviceDisabled = isDeviceDisabled,
                    isPickedUp = false,
                    isDragSource = false,
                    isDropTarget = false,
                    modifier =
                        Modifier
                            .absoluteOffset { IntOffset(left, top) }
                            .size(widthDp, heightDp),
                )
            }
        }

        PadTableGridLines(layout = layout)
    }
}
