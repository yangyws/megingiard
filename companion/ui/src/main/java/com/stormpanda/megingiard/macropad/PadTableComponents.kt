package com.stormpanda.megingiard.macropad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.stormpanda.megingiard.ui.LocalAppColors
import kotlinx.coroutines.withTimeoutOrNull
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
internal val PTC_TABLE_GRID_LINE_COLOR = Color(0x66FFFFFF)
internal val PTC_TABLE_LABEL_RESERVE = 16.dp

internal val PTC_TABLE_THICK_BORDER_WIDTH = 2.5.dp
internal val PTC_TABLE_THICK_BORDER_COLOR = Color.Black
internal val PTC_TABLE_THICK_BORDER_CELL_PADDING = 2.5.dp
internal val PTC_TABLE_THICK_BORDER_OUTER_PADDING = PTC_TABLE_THICK_BORDER_CELL_PADDING + PTC_TABLE_THICK_BORDER_WIDTH
internal val PTC_TABLE_THICK_BORDER_INSET_PADDING = 4.dp
internal val PTC_TABLE_THICK_BORDER_ICON_SIZE = 20.dp
internal val PTC_TABLE_THICK_BORDER_FACE_REDUCTION = 12.dp

internal val PTC_TABLE_BASE_BG = Color(0xFF26262C)
internal val PTC_TABLE_BASE_BG_PRESSED = Color(0xFF3E3E48)

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
    val isThickBorder = layout.gridShowBorders

    Box(
        modifier =
            modifier
                .then(
                    if (isThickBorder) {
                        Modifier
                            .padding(PTC_TABLE_THICK_BORDER_CELL_PADDING)
                            .border(
                                width = PTC_TABLE_THICK_BORDER_WIDTH,
                                color = PTC_TABLE_THICK_BORDER_COLOR,
                                shape = shape,
                            )
                    } else {
                        Modifier
                    },
                )
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
                    if (layout.gridShowButtonBg) {
                        val baseColor = if (isPressed) PTC_TABLE_BASE_BG_PRESSED else PTC_TABLE_BASE_BG
                        drawRect(baseColor)
                        if (button.buttonBgColor != null || layout.buttonBgColor !is ColorOption.Neutral) {
                            val alpha = if (isPressed) 0.55f else 0.30f
                            val effectiveCustomBg = if (isPickedUp) PTC_TABLE_SELECTED_BG else bg.copy(alpha = alpha)
                            drawRect(effectiveCustomBg)
                        } else if (isPickedUp) {
                            drawRect(PTC_TABLE_SELECTED_BG)
                        }
                    } else {
                        if (isPressed) {
                            drawRect(PTC_TABLE_BASE_BG_PRESSED.copy(alpha = 0.5f))
                        } else if (isPickedUp) {
                            drawRect(PTC_TABLE_SELECTED_BG)
                        }
                    }
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
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(
                        if (isThickBorder) {
                            Modifier.padding(PTC_TABLE_THICK_BORDER_INSET_PADDING)
                        } else {
                            Modifier
                        },
                    )
                    .graphicsLayer {
                        val scale = if (isPressed) 0.93f else 1.0f
                        scaleX = scale
                        scaleY = scale
                    },
            contentAlignment = Alignment.Center,
        ) {
            PadButtonContent(
                btn = button,
                effectiveTextTint = text,
                iconSize = if (isThickBorder) PTC_TABLE_THICK_BORDER_ICON_SIZE else PTC_TABLE_ICON_SIZE,
                isTrackpoint = false,
                effectiveContentAccent = accentColor,
            )
        }
    }
}

@Composable
internal fun PadTableGridLines(layout: PadLayout) {
    if (layout.gridShowBorders) return
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

    val borderPaddingPx = if (layout.gridShowBorders) with(density) { PTC_TABLE_THICK_BORDER_OUTER_PADDING.toPx() } else 0f
    val availW = (w - borderPaddingPx * 2).coerceAtLeast(0f)
    val availH = (h - borderPaddingPx * 2).coerceAtLeast(0f)
    val cellW = availW / cols
    val cellH = availH / rows

    val visibleButtons =
        remember(layout, cols, rows, isPeekActive) {
            val base = layout.buttons.filter { it.isWithinGrid(cols, rows) }
            if (isPeekActive) base.filter { it.action is PadAction.BackgroundPeek } else base
        }

    val emptyCells =
        remember(layout, cols, rows) {
            val list = mutableListOf<Pair<Int, Int>>()
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    if (GridLayoutMath.buttonAt(layout, c, r) == null) {
                        list.add(c to r)
                    }
                }
            }
            list
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
                ),
    ) {
        emptyCells.forEach { (col, row) ->
            val left = (cellW * col).roundToInt()
            val top = (cellH * row).roundToInt()
            val widthDp = with(density) { cellW.toDp() }
            val heightDp = with(density) { cellH.toDp() }
            val shape = RoundedCornerShape(PTC_TABLE_CELL_CORNER_RADIUS)

            Box(
                modifier =
                    Modifier
                        .absoluteOffset { IntOffset(left, top) }
                        .size(widthDp, heightDp)
                        .then(
                            if (layout.gridShowBorders) {
                                Modifier
                                    .padding(PTC_TABLE_THICK_BORDER_CELL_PADDING)
                                    .border(
                                        width = PTC_TABLE_THICK_BORDER_WIDTH,
                                        color = PTC_TABLE_THICK_BORDER_COLOR,
                                        shape = shape,
                                    )
                            } else {
                                Modifier
                            },
                        )
                        .then(
                            if (layout.gridShowButtonBg) {
                                Modifier.background(PTC_TABLE_BASE_BG, shape)
                            } else {
                                Modifier
                            },
                        )
                        .clip(shape),
            )
        }

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
    }
}

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

@Composable
internal fun PadTableGrid(
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
            val requestedRadiusPx = with(density) { PTC_TABLE_CELL_CORNER_RADIUS.toPx() }
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
                    val col = button.gridCol ?: 0
                    val row = button.gridRow ?: 0
                    val cellShape =
                        if (row in cellShapes.indices && col in cellShapes[row].indices) {
                            cellShapes[row][col]
                        } else {
                            RoundedCornerShape(PTC_TABLE_CELL_CORNER_RADIUS)
                        }
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
                        if (row in cellShapes.indices && col in cellShapes[row].indices) {
                            cellShapes[row][col]
                        } else {
                            RoundedCornerShape(PTC_TABLE_CELL_CORNER_RADIUS)
                        }

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
                            size = PTC_TABLE_ADD_ICON_SIZE,
                            tint = colors.onSurfaceSecondary.copy(alpha = PTC_TABLE_EMPTY_ICON_ALPHA),
                            filled = false,
                        )
                    }
                }

                if (isDraggingTarget) {
                    val sButton = sourceButton ?: return@Layout
                    val dragCell = dOver ?: return@Layout
                    val pressCellPos = pCell ?: return@Layout
                    val sCol = sButton.gridCol ?: 0
                    val sRow = sButton.gridRow ?: 0
                    val touchOffsetCol = pressCellPos.first - sCol
                    val touchOffsetRow = pressCellPos.second - sRow
                    val tCol = dragCell.first - touchOffsetCol
                    val tRow = dragCell.second - touchOffsetRow

                    val isValidTarget = GridLayoutMath.canMoveButton(layout, pressCellPos, dragCell)
                    val targetShape =
                        if (tRow in cellShapes.indices && tCol in cellShapes[tRow].indices) {
                            cellShapes[tRow][tCol]
                        } else {
                            RoundedCornerShape(PTC_TABLE_CELL_CORNER_RADIUS)
                        }

                    Box(
                        modifier =
                            Modifier
                                .background(
                                    color = if (isValidTarget) Color.Transparent else colors.error.copy(alpha = 0.22f),
                                    shape = targetShape,
                                )
                                .border(
                                    width = PTC_TABLE_DROP_BORDER_WIDTH,
                                    color = if (isValidTarget) PTC_TABLE_SELECTED_BORDER else colors.error,
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
                    sourceButton != null &&
                    dOver != pCell

            var idx = 0
            val placeables = mutableListOf<Triple<Placeable, Int, Int>>()

            uniqueButtons.forEach { button ->
                val col = button.gridCol ?: 0
                val row = button.gridRow ?: 0
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

            if (isDraggingTarget && idx < measurables.size) {
                val sButton = sourceButton ?: return@Layout layout(0, 0) {}
                val dragCell = dOver ?: return@Layout layout(0, 0) {}
                val pressCellPos = pCell ?: return@Layout layout(0, 0) {}
                val sCol = sButton.gridCol ?: 0
                val sRow = sButton.gridRow ?: 0
                val touchOffsetCol = pressCellPos.first - sCol
                val touchOffsetRow = pressCellPos.second - sRow
                val tCol = dragCell.first - touchOffsetCol
                val tRow = dragCell.second - touchOffsetRow
                val tColSpan = sButton.effectiveColSpan
                val tRowSpan = sButton.effectiveRowSpan

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
