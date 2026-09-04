package com.stormpanda.megingiard.macropad

/**
 * Geometry for [PadLayoutMode.GRID] layouts, and the single definition of how much
 * room any pad button occupies.
 *
 * Kept here, in `:core`, so the runtime surface, the hit-test engine and the editor
 * canvas all derive cell geometry from one place. The free-placement path used to
 * have its button unit duplicated as three separate constants (`MP_BUTTON_UNIT_DP`,
 * `MP_BUTTON_UNIT_DP_VALUE`, `ED_BUTTON_UNIT_DP`); those now all read [BUTTON_UNIT_DP]
 * from here, because table mode already proved what the duplication costs — the editor
 * drew table buttons filling their cell while the live pad drew a 60dp chip in the
 * middle of it, and neither side was wrong about its own constant.
 *
 * Pure functions with no Android dependency, so they are unit-testable without
 * Robolectric.
 */
object GridLayoutMath {
    /**
     * Edge length in dp of a 1x1 free-placement button.
     *
     * Raw dp rather than a Compose `Dp` because `:core` has no Compose dependency;
     * every caller converts with its own density.
     */
    const val BUTTON_UNIT_DP = 60f

    /**
     * Width in dp of one grid line.
     *
     * Table cells butt directly against each other — there is no inset — so this line *is*
     * the separation between two neighbours, and it also stands in for the border a table
     * button no longer draws for itself. Both renderers read it from here; it used to be
     * written twice (`MP_TABLE_BORDER_WIDTH` and `PC_TABLE_BORDER_WIDTH`), which is the
     * duplication this object exists to stop.
     *
     * 1dp read as a hairline on the 3.92" panel — technically present, not legible as a
     * frame. Widening it is free geometrically: [gridLineRect] centres the pen on the
     * boundary and pushes it inside at the rim, so a thicker line eats equally into both
     * neighbours rather than shifting either of them.
     */
    const val GRID_LINE_WIDTH_DP = 2f

    /**
     * Corner radius in dp of a table button's face.
     *
     * The face is rounded; **the ruling from [gridLines] is not**. A cell still runs edge
     * to edge, the boundary is still ruled exactly once, and neighbours still share that
     * one line — the radius only pulls each face away from its own four corners, which is
     * what a corner radius is. Rounding the ruling instead would mean each button stroking
     * its own outline, which is precisely the two-lines-per-shared-edge this geometry
     * exists to prevent.
     *
     * **A corner only takes this radius when both edges meeting at it are ruled** — see
     * [cellCorners]. A radius is a way of finishing a drawn corner, so where there is no
     * corner drawn there is nothing to finish, and curving away from a boundary nobody
     * ruled just opens a notch in an otherwise continuous surface.
     *
     * **The cost, stated plainly:** where four ruled edges meet, each face retreats by this
     * radius, so a small pocket of whatever is behind the table shows through at that
     * crossing. The deepest point of that pocket is `radius * (sqrt(2) - 1)`, about 3 dp
     * here. The same happens at the table's own four outer corners, where the ruling keeps
     * its square corner while the face inside it curves away. Both are the price of a
     * rounded face on a shared line, and both are paid uniformly, so they read as a corner
     * treatment rather than as a gap.
     *
     * It also clips the corners off a button's image, which is why this constant was
     * removed when the shared ruling landed. It comes back because the corners are now the
     * *only* thing clipped — there is no inset, so two neighbouring images still meet along
     * the whole of their shared edge, parted by one line rather than by a gap.
     *
     * Raw dp, like every other constant here; callers convert with their own density and
     * clamp with [faceCornerRadiusPx].
     */
    const val CELL_CORNER_RADIUS_DP = 8f

    /**
     * [requestedRadiusPx] clamped to what a [faceWidthPx] x [faceHeightPx] face can carry.
     *
     * A radius over half the shorter side is not a rounder rectangle, it is an undefined
     * one: Compose renormalises overlapping corners, and the two renderers would then be
     * relying on that renormalisation agreeing rather than on one number. At 8x6 — the
     * densest table allowed — a cell is about 30 x 34 dp, so the clamp does not bite; it
     * bites on a canvas measured at zero for a frame, and it would bite again if a merged
     * button ever made a face smaller rather than larger.
     *
     * Returns 0 for a face with no area and for a non-finite or negative request, so a
     * degenerate measurement produces a square corner rather than a NaN handed to the
     * draw call.
     */
    fun faceCornerRadiusPx(
        faceWidthPx: Float,
        faceHeightPx: Float,
        requestedRadiusPx: Float,
    ): Float {
        if (!requestedRadiusPx.isFinite() || requestedRadiusPx <= 0f) return 0f
        val shortest = minOf(faceWidthPx, faceHeightPx)
        if (!shortest.isFinite() || shortest <= 0f) return 0f
        return requestedRadiusPx.coerceAtMost(shortest / 2f)
    }

    /**
     * Normalised centre of the cell at [col], [row] in a [cols] x [rows] table.
     *
     * Returns the same 0..1 centre-point convention [PadButton.posX]/[PadButton.posY]
     * use, which is what lets table buttons flow through the existing rendering and
     * hit-testing paths untouched.
     */
    fun cellCenter(
        col: Int,
        row: Int,
        cols: Int,
        rows: Int,
    ): Pair<Float, Float> = cellCenter(col, row, 1, 1, cols, rows)

    /**
     * Normalised centre of the cell block at [col], [row] spanning [colSpan] x [rowSpan]
     * in a [cols] x [rows] table.
     */
    fun cellCenter(
        col: Int,
        row: Int,
        colSpan: Int,
        rowSpan: Int,
        cols: Int,
        rows: Int,
    ): Pair<Float, Float> {
        val safeCols = cols.coerceAtLeast(1)
        val safeRows = rows.coerceAtLeast(1)
        val cs = colSpan.coerceAtLeast(1)
        val rs = rowSpan.coerceAtLeast(1)
        val c = col.coerceIn(0, safeCols - 1)
        val r = row.coerceIn(0, safeRows - 1)
        val cx = (c + cs / 2.0f) / safeCols
        val cy = (r + rs / 2.0f) / safeRows
        return cx to cy
    }

    /**
     * The cell containing normalised point ([x], [y]), or null if it falls outside
     * the unit square.
     */
    fun cellAt(
        x: Float,
        y: Float,
        cols: Int,
        rows: Int,
    ): Pair<Int, Int>? {
        if (x < 0f || x > 1f || y < 0f || y > 1f) return null
        val safeCols = cols.coerceAtLeast(1)
        val safeRows = rows.coerceAtLeast(1)
        // coerce guards the exact-1.0 edge, which would otherwise index one past the end.
        val col = (x * safeCols).toInt().coerceIn(0, safeCols - 1)
        val row = (y * safeRows).toInt().coerceIn(0, safeRows - 1)
        return col to row
    }

    /**
     * Buttons of [layout] that should be drawn and hit-tested, with table buttons
     * repositioned onto their cell centres and sized by their cell bounds.
     */
    fun resolveButtons(layout: PadLayout): List<PadButton> {
        if (!layout.isGridMode) return layout.buttons

        val cols = layout.effectiveGridCols
        val rows = layout.effectiveGridRows
        return layout.buttons
            .filter { it.isWithinGrid(cols, rows) }
            .map { button ->
                val col = button.gridCol!!
                val row = button.gridRow!!
                val cs = button.effectiveColSpan
                val rs = button.effectiveRowSpan
                val (cx, cy) = cellCenter(col, row, cs, rs, cols, rows)
                button.copy(
                    posX = cx,
                    posY = cy,
                    resolvedCell = cellBounds(col, row, cs, rs, cols, rows),
                )
            }
    }

    /**
     * First cell with no button on it, scanning row by row, or null when the table is
     * full. Takes into account cells covered by merged buttons.
     */
    fun firstFreeCell(layout: PadLayout): Pair<Int, Int>? {
        val cols = layout.effectiveGridCols
        val rows = layout.effectiveGridRows
        val taken = mutableSetOf<Pair<Int, Int>>()
        for (button in layout.buttons) {
            val c = button.gridCol ?: continue
            val r = button.gridRow ?: continue
            if (button.isWithinGrid(cols, rows)) {
                for (dc in 0 until button.effectiveColSpan) {
                    for (dr in 0 until button.effectiveRowSpan) {
                        taken.add((c + dc) to (r + dr))
                    }
                }
            }
        }

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if ((col to row) !in taken) return col to row
            }
        }
        return null
    }

    /** The button occupying ([col], [row]), taking span into account, or null when empty. */
    fun buttonAt(
        layout: PadLayout,
        col: Int,
        row: Int,
    ): PadButton? =
        layout.buttons.firstOrNull { button ->
            val c = button.gridCol ?: return@firstOrNull false
            val r = button.gridRow ?: return@firstOrNull false
            val cs = button.effectiveColSpan
            val rs = button.effectiveRowSpan
            col in c until (c + cs) && row in r until (r + rs)
        }

    /**
     * Finds the nearest existing neighbor button in direction ([dirX], [dirY]) on the 2D grid of [layout].
     */
    fun findNeighborButton(
        layout: PadLayout,
        currentBtn: PadButton,
        dirX: Int,
        dirY: Int,
    ): PadButton? {
        if (!layout.isGridMode) return null
        val cols = layout.effectiveGridCols
        val rows = layout.effectiveGridRows
        val curCol = currentBtn.gridCol ?: 0
        val curRow = currentBtn.gridRow ?: 0
        val curColSpan = currentBtn.effectiveColSpan
        val curRowSpan = currentBtn.effectiveRowSpan

        if (dirX != 0) {
            val step = if (dirX > 0) 1 else -1
            var targetC = if (dirX > 0) curCol + curColSpan else curCol - 1
            while (targetC in 0 until cols) {
                for (r in curRow until (curRow + curRowSpan)) {
                    val found = buttonAt(layout, targetC, r)
                    if (found != null && found.id != currentBtn.id) {
                        return found
                    }
                }
                targetC += step
            }
        } else if (dirY != 0) {
            val step = if (dirY > 0) 1 else -1
            var targetR = if (dirY > 0) curRow + curRowSpan else curRow - 1
            while (targetR in 0 until rows) {
                for (c in curCol until (curCol + curColSpan)) {
                    val found = buttonAt(layout, c, targetR)
                    if (found != null && found.id != currentBtn.id) {
                        return found
                    }
                }
                targetR += step
            }
        }
        return null
    }

    /**
     * Checks whether [button] (or button with [buttonId]) in [layout] can be resized to [newColSpan] x [newRowSpan]
     * without going outside grid bounds or overlapping with another button.
     */
    fun canSpanButton(
        layout: PadLayout,
        button: PadButton?,
        newColSpan: Int,
        newRowSpan: Int,
    ): Boolean {
        if (!layout.isGridMode) return true
        val cols = layout.effectiveGridCols
        val rows = layout.effectiveGridRows

        if (newColSpan < 1 || newRowSpan < 1) return false
        if (newColSpan > cols || newRowSpan > rows) return false

        val btnId = button?.id ?: ""
        val col = button?.gridCol ?: layout.buttons.firstOrNull { it.id == btnId }?.gridCol ?: firstFreeCell(layout)?.first ?: 0
        val row = button?.gridRow ?: layout.buttons.firstOrNull { it.id == btnId }?.gridRow ?: firstFreeCell(layout)?.second ?: 0

        if (col < 0 || row < 0 || col + newColSpan > cols || row + newRowSpan > rows) return false

        for (other in layout.buttons) {
            if (other.id == btnId) continue
            val oc = other.gridCol ?: continue
            val or = other.gridRow ?: continue
            if (!other.isWithinGrid(cols, rows)) continue

            val oColSpan = other.effectiveColSpan
            val oRowSpan = other.effectiveRowSpan

            val overlapCol = maxOf(col, oc) < minOf(col + newColSpan, oc + oColSpan)
            val overlapRow = maxOf(row, or) < minOf(row + newRowSpan, or + oRowSpan)
            if (overlapCol && overlapRow) return false
        }
        return true
    }

    /**
     * Attempts to find a valid anchor column for [targetColSpan].
     * Checks rightwards first (same [currentCol]); if blocked, checks leftwards (shifts [currentCol] left).
     * Returns the valid anchor col, or null if no valid non-overlapping placement exists without displacing other buttons.
     */
    fun findExpandedSpanCol(
        layout: PadLayout,
        button: PadButton,
        targetColSpan: Int,
        rowSpan: Int = button.effectiveRowSpan,
        currentCol: Int = button.gridCol ?: 0,
        currentRow: Int = button.gridRow ?: 0,
    ): Int? {
        if (!layout.isGridMode) return currentCol
        val cols = layout.effectiveGridCols
        if (targetColSpan < 1 || targetColSpan > cols) return null

        // 1. Try expanding rightwards (anchor stays at currentCol)
        if (currentCol + targetColSpan <= cols) {
            val candidate = button.copy(gridCol = currentCol, gridRow = currentRow)
            if (canSpanButton(layout, candidate, targetColSpan, rowSpan)) {
                return currentCol
            }
        }

        // 2. Try expanding leftwards (anchor shifted left)
        for (candidateCol in (currentCol - 1) downTo 0) {
            if (candidateCol + targetColSpan <= cols) {
                val candidate = button.copy(gridCol = candidateCol, gridRow = currentRow)
                if (canSpanButton(layout, candidate, targetColSpan, rowSpan)) {
                    return candidateCol
                }
            }
        }

        return null
    }

    /**
     * Attempts to find a valid anchor row for [targetRowSpan].
     * Checks downwards first (same [currentRow]); if blocked, checks upwards (shifts [currentRow] up).
     * Returns the valid anchor row, or null if no valid non-overlapping placement exists without displacing other buttons.
     */
    fun findExpandedSpanRow(
        layout: PadLayout,
        button: PadButton,
        targetRowSpan: Int,
        colSpan: Int = button.effectiveColSpan,
        currentCol: Int = button.gridCol ?: 0,
        currentRow: Int = button.gridRow ?: 0,
    ): Int? {
        if (!layout.isGridMode) return currentRow
        val rows = layout.effectiveGridRows
        if (targetRowSpan < 1 || targetRowSpan > rows) return null

        // 1. Try expanding downwards (anchor stays at currentRow)
        if (currentRow + targetRowSpan <= rows) {
            val candidate = button.copy(gridCol = currentCol, gridRow = currentRow)
            if (canSpanButton(layout, candidate, colSpan, targetRowSpan)) {
                return currentRow
            }
        }

        // 2. Try expanding upwards (anchor shifted up)
        for (candidateRow in (currentRow - 1) downTo 0) {
            if (candidateRow + targetRowSpan <= rows) {
                val candidate = button.copy(gridCol = currentCol, gridRow = candidateRow)
                if (canSpanButton(layout, candidate, colSpan, targetRowSpan)) {
                    return candidateRow
                }
            }
        }

        return null
    }

data class ResizeButtonResult(
    val layout: PadLayout,
    val movedButtons: List<PadButton>,
    val replacedButtons: List<PadButton>,
)

    /**
     * Resizes (spans) [resizedButton] in [layout].
     */
    fun resizeOrSpanButton(
        layout: PadLayout,
        resizedButton: PadButton,
    ): ResizeButtonResult {
        if (!layout.isGridMode) {
            val isNew = layout.buttons.none { it.id == resizedButton.id }
            val updated =
                if (isNew) {
                    layout.buttons + resizedButton
                } else {
                    layout.buttons.map { if (it.id == resizedButton.id) resizedButton else it }
                }
            return ResizeButtonResult(
                layout = layout.copy(buttons = updated),
                movedButtons = emptyList(),
                replacedButtons = emptyList(),
            )
        }

        val cols = layout.effectiveGridCols
        val rows = layout.effectiveGridRows
        val btnCol = (resizedButton.gridCol ?: 0).coerceIn(0, cols - 1)
        val btnRow = (resizedButton.gridRow ?: 0).coerceIn(0, rows - 1)
        val colSpan = resizedButton.effectiveColSpan.coerceIn(1, cols - btnCol)
        val rowSpan = resizedButton.effectiveRowSpan.coerceIn(1, rows - btnRow)
        val adjustedButton =
            resizedButton.copy(
                gridCol = btnCol,
                gridRow = btnRow,
                colSpan = colSpan,
                rowSpan = rowSpan,
            )

        // Set of occupied cells
        val occupied = mutableSetOf<Pair<Int, Int>>()
        for (c in btnCol until (btnCol + colSpan)) {
            for (r in btnRow until (btnRow + rowSpan)) {
                occupied.add(c to r)
            }
        }

        val otherButtons = layout.buttons.filter { it.id != adjustedButton.id && it.isWithinGrid(cols, rows) }
        val colliding = mutableListOf<PadButton>()
        val nonColliding = mutableListOf<PadButton>()

        for (other in otherButtons) {
            val oc = other.gridCol ?: continue
            val or = other.gridRow ?: continue
            val ocs = other.effectiveColSpan
            val ors = other.effectiveRowSpan

            val overlaps =
                maxOf(btnCol, oc) < minOf(btnCol + colSpan, oc + ocs) &&
                    maxOf(btnRow, or) < minOf(btnRow + rowSpan, or + ors)
            if (overlaps) {
                colliding.add(other)
            } else {
                nonColliding.add(other)
                for (c in oc until (oc + ocs)) {
                    for (r in or until (or + ors)) {
                        occupied.add(c to r)
                    }
                }
            }
        }

        val movedButtons = mutableListOf<PadButton>()
        val replacedButtons = mutableListOf<PadButton>()
        val placedOtherButtons = nonColliding.toMutableList()

        for (target in colliding) {
            val tcs = target.effectiveColSpan
            val trs = target.effectiveRowSpan
            var placed = false
            for (r in 0..(rows - trs)) {
                for (c in 0..(cols - tcs)) {
                    var canFit = true
                    for (dc in 0 until tcs) {
                        for (dr in 0 until trs) {
                            if ((c + dc to r + dr) in occupied) {
                                canFit = false
                                break
                            }
                        }
                        if (!canFit) break
                    }
                    if (canFit) {
                        val moved = target.copy(gridCol = c, gridRow = r)
                        for (dc in 0 until tcs) {
                            for (dr in 0 until trs) {
                                occupied.add(c + dc to r + dr)
                            }
                        }
                        placedOtherButtons.add(moved)
                        movedButtons.add(moved)
                        placed = true
                        break
                    }
                }
                if (placed) break
            }

            if (!placed) {
                replacedButtons.add(target)
            }
        }

        val finalButtons = placedOtherButtons + adjustedButton
        return ResizeButtonResult(
            layout = layout.copy(buttons = finalButtons),
            movedButtons = movedButtons,
            replacedButtons = replacedButtons,
        )
    }

    /**
     * Checks whether moving the button at cell [from] to cell [to] in [layout] is valid
     * (i.e. neither the moved source nor swapped target overflow grid bounds or overlap other buttons).
     */
    fun canMoveButton(
        layout: PadLayout,
        from: Pair<Int, Int>,
        to: Pair<Int, Int>,
    ): Boolean {
        if (!layout.isGridMode) return true

        val source = buttonAt(layout, from.first, from.second) ?: return false
        val sourceAnchorCol = source.gridCol ?: return false
        val sourceAnchorRow = source.gridRow ?: return false

        val touchOffsetCol = from.first - sourceAnchorCol
        val touchOffsetRow = from.second - sourceAnchorRow

        val newAnchorCol = to.first - touchOffsetCol
        val newAnchorRow = to.second - touchOffsetRow
        if (newAnchorCol == sourceAnchorCol && newAnchorRow == sourceAnchorRow) return true

        val cols = layout.effectiveGridCols
        val rows = layout.effectiveGridRows

        val sColSpan = source.effectiveColSpan
        val sRowSpan = source.effectiveRowSpan
        val newMaxCol = newAnchorCol + sColSpan
        val newMaxRow = newAnchorRow + sRowSpan

        if (newAnchorCol < 0 || newAnchorRow < 0 || newMaxCol > cols || newMaxRow > rows) {
            return false
        }

        val collidingButtons = layout.buttons.filter { button ->
            if (button.id == source.id) return@filter false
            val c = button.gridCol ?: return@filter false
            val r = button.gridRow ?: return@filter false
            if (!button.isWithinGrid(cols, rows)) return@filter false
            val cs = button.effectiveColSpan
            val rs = button.effectiveRowSpan

            val overlapCol = maxOf(newAnchorCol, c) < minOf(newMaxCol, c + cs)
            val overlapRow = maxOf(newAnchorRow, r) < minOf(newMaxRow, r + rs)
            overlapCol && overlapRow
        }

        // Pure swap / move-to-empty rule (no push cascading):
        // 1. Target area completely empty -> source moves to new anchor.
        // 2. Target area collides with exactly 1 button -> 1-to-1 anchor swap (if both fit without overlap/out-of-bounds).
        // 3. Target area collides with multiple buttons -> blocked (no push cascading).
        if (collidingButtons.size > 1) {
            return false
        }

        val updatedButtons = layout.buttons.mapNotNull { button ->
            val c = button.gridCol ?: return@mapNotNull null
            val r = button.gridRow ?: return@mapNotNull null
            if (!button.isWithinGrid(cols, rows)) return@mapNotNull null

            when {
                button.id == source.id -> {
                    button.copy(gridCol = newAnchorCol, gridRow = newAnchorRow)
                }

                collidingButtons.isNotEmpty() && button.id == collidingButtons[0].id -> {
                    val cs = button.effectiveColSpan
                    val rs = button.effectiveRowSpan
                    val targetC =
                        when {
                            newAnchorCol > sourceAnchorCol -> sourceAnchorCol
                            newAnchorCol < sourceAnchorCol -> sourceAnchorCol + sColSpan - cs
                            else -> sourceAnchorCol
                        }
                    val targetR =
                        when {
                            newAnchorRow > sourceAnchorRow -> sourceAnchorRow
                            newAnchorRow < sourceAnchorRow -> sourceAnchorRow + sRowSpan - rs
                            else -> sourceAnchorRow
                        }
                    button.copy(gridCol = targetC, gridRow = targetR)
                }

                else -> button
            }
        }

        for (button in updatedButtons) {
            val c = button.gridCol!!
            val r = button.gridRow!!
            val cs = button.effectiveColSpan
            val rs = button.effectiveRowSpan

            if (c < 0 || r < 0 || c + cs > cols || r + rs > rows) {
                return false
            }
        }

        for (i in updatedButtons.indices) {
            val b1 = updatedButtons[i]
            val c1 = b1.gridCol!!
            val r1 = b1.gridRow!!
            val cs1 = b1.effectiveColSpan
            val rs1 = b1.effectiveRowSpan

            for (j in i + 1 until updatedButtons.size) {
                val b2 = updatedButtons[j]
                val c2 = b2.gridCol!!
                val r2 = b2.gridRow!!
                val cs2 = b2.effectiveColSpan
                val rs2 = b2.effectiveRowSpan

                val overlapCol = maxOf(c1, c2) < minOf(c1 + cs1, c2 + cs2)
                val overlapRow = maxOf(r1, r2) < minOf(r1 + rs1, r2 + rs2)
                if (overlapCol && overlapRow) {
                    return false
                }
            }
        }

        return true
    }

    /**
     * Moves the button on cell [from] to cell [to].
     */
    fun moveButton(
        layout: PadLayout,
        from: Pair<Int, Int>,
        to: Pair<Int, Int>,
    ): PadLayout {
        val source = buttonAt(layout, from.first, from.second) ?: return layout
        val sourceAnchorCol = source.gridCol ?: return layout
        val sourceAnchorRow = source.gridRow ?: return layout

        val touchOffsetCol = from.first - sourceAnchorCol
        val touchOffsetRow = from.second - sourceAnchorRow

        val newAnchorCol = to.first - touchOffsetCol
        val newAnchorRow = to.second - touchOffsetRow

        if (newAnchorCol == sourceAnchorCol && newAnchorRow == sourceAnchorRow) return layout

        if (!canMoveButton(layout, from, to)) return layout

        val cols = layout.effectiveGridCols
        val rows = layout.effectiveGridRows
        val sColSpan = source.effectiveColSpan
        val sRowSpan = source.effectiveRowSpan

        val collidingButtons = layout.buttons.filter { button ->
            if (button.id == source.id) return@filter false
            val c = button.gridCol ?: return@filter false
            val r = button.gridRow ?: return@filter false
            if (!button.isWithinGrid(cols, rows)) return@filter false
            val cs = button.effectiveColSpan
            val rs = button.effectiveRowSpan

            val overlapCol = maxOf(newAnchorCol, c) < minOf(newAnchorCol + sColSpan, c + cs)
            val overlapRow = maxOf(newAnchorRow, r) < minOf(newAnchorRow + sRowSpan, r + rs)
            overlapCol && overlapRow
        }

        val moved =
            layout.buttons.map { button ->
                when {
                    button.id == source.id -> {
                        button.copy(gridCol = newAnchorCol, gridRow = newAnchorRow)
                    }

                    collidingButtons.isNotEmpty() && button.id == collidingButtons[0].id -> {
                        val cs = button.effectiveColSpan
                        val rs = button.effectiveRowSpan
                        val targetC =
                            when {
                                newAnchorCol > sourceAnchorCol -> sourceAnchorCol
                                newAnchorCol < sourceAnchorCol -> sourceAnchorCol + sColSpan - cs
                                else -> sourceAnchorCol
                            }
                        val targetR =
                            when {
                                newAnchorRow > sourceAnchorRow -> sourceAnchorRow
                                newAnchorRow < sourceAnchorRow -> sourceAnchorRow + sRowSpan - rs
                                else -> sourceAnchorRow
                            }
                        button.copy(gridCol = targetC, gridRow = targetR)
                    }

                    else -> button
                }
            }
        return layout.copy(buttons = moved)
    }

    /**
     * Swaps the button at [from] with the button at [to], or moves the button at [from] to [to] if [to] is empty.
     */
    fun swapOrMoveButton(
        layout: PadLayout,
        from: Pair<Int, Int>,
        to: Pair<Int, Int>,
    ): PadLayout {
        if (from == to) return layout
        return moveButton(layout, from, to)
    }

    fun cellBounds(
        col: Int,
        row: Int,
        cols: Int,
        rows: Int,
    ): CellBounds = cellBounds(col, row, 1, 1, cols, rows)

    /**
     * Normalised left/top/right/bottom of the cell block at [col], [row] spanning [colSpan] x [rowSpan].
     */
    fun cellBounds(
        col: Int,
        row: Int,
        colSpan: Int,
        rowSpan: Int,
        cols: Int,
        rows: Int,
    ): CellBounds {
        val safeCols = cols.coerceAtLeast(1)
        val safeRows = rows.coerceAtLeast(1)
        val cs = colSpan.coerceAtLeast(1)
        val rs = rowSpan.coerceAtLeast(1)
        val c = col.coerceIn(0, safeCols - 1)
        val r = row.coerceIn(0, safeRows - 1)
        val maxRight = (c + cs).coerceAtMost(safeCols)
        val maxBottom = (r + rs).coerceAtMost(safeRows)
        return CellBounds(
            left = c.toFloat() / safeCols,
            top = r.toFloat() / safeRows,
            right = maxRight.toFloat() / safeCols,
            bottom = maxBottom.toFloat() / safeRows,
        )
    }

    /**
     * Pixel rect [cell] covers on a [canvasWidthPx] x [canvasHeightPx] surface.
     *
     * The whole cell, edge to edge. Neighbouring cells therefore share a boundary
     * exactly, which is what lets two adjacent button faces meet with no seam — the
     * grid lines from [gridLines] are drawn *over* that boundary rather than a gap
     * being left for them.
     *
     * This used to take an inset that both the cell frame and the button face were
     * derived from. The inset is gone: per-cell frames are gone with it, so the only
     * thing the gap was buying was a visible break between two images. It stays gone now
     * that faces are rounded again — the rectangle handed out here is the full cell, and
     * [CELL_CORNER_RADIUS_DP] shapes the face drawn inside it rather than shrinking it.
     */
    fun cellRect(
        cell: CellBounds,
        canvasWidthPx: Float,
        canvasHeightPx: Float,
    ): PadRect =
        PadRect(
            left = cell.left * canvasWidthPx,
            top = cell.top * canvasHeightPx,
            width = (cell.width * canvasWidthPx).coerceAtLeast(0f),
            height = (cell.height * canvasHeightPx).coerceAtLeast(0f),
        )

    /**
     * Every grid line of [layout], as normalised 0..1 segments.
     *
     * **One rule decides the whole grid: a line is drawn where two adjacent cells belong
     * to different buttons.** The outer edge of the table always qualifies (there is no
     * neighbour on the far side), two empty cells qualify (neither belongs to a button,
     * so they are not the same button), and two cells held by the *same* button do not —
     * which is how a merged button loses its interior ruling without anything having to
     * special-case merging.
     *
     * Collinear neighbouring edges are merged into one segment, so a plain 5x4 table is
     * 6 verticals and 5 horizontals rather than 49 unit strokes. That keeps the output
     * small enough to assert on directly in tests, and it means a line never has a seam
     * in the middle of it where two segments abut.
     *
     * @param outlineEmptyCells keep the edges of empty cells even when
     *   [PadLayout.gridShowBorders] is off. The editor passes `true`: there, the outline
     *   of an empty cell is the only thing saying "a button can go here". The live pad
     *   passes `false`, so hiding the grid there really does hide all of it.
     */
    fun gridLines(
        layout: PadLayout,
        outlineEmptyCells: Boolean,
    ): List<GridLine> {
        if (!layout.isGridMode) return emptyList()
        val cols = layout.effectiveGridCols
        val rows = layout.effectiveGridRows
        if (!layout.gridShowBorders && !outlineEmptyCells) return emptyList()

        // Occupancy is read once. buttonAt() is a linear scan, and the edge test below
        // asks about every cell twice.
        val occupant = List(rows) { row -> List(cols) { col -> buttonAt(layout, col, row)?.id } }
        val ruling = EdgeRuling(layout, outlineEmptyCells) { col, row -> occupant[row][col] }

        fun drawsEdge(
            aCol: Int,
            aRow: Int,
            bCol: Int,
            bRow: Int,
        ): Boolean = ruling.isRuled(aCol, aRow, bCol, bRow)

        val lines = mutableListOf<GridLine>()

        // Verticals: the line at column boundary `col` separates (col - 1, row) from (col, row).
        for (col in 0..cols) {
            val x = col.toFloat() / cols
            var runStart = -1
            for (row in 0..rows) {
                val drawn = row < rows && drawsEdge(col - 1, row, col, row)
                if (drawn && runStart < 0) runStart = row
                if (!drawn && runStart >= 0) {
                    lines += GridLine(x, runStart.toFloat() / rows, x, row.toFloat() / rows)
                    runStart = -1
                }
            }
        }

        // Horizontals: the line at row boundary `row` separates (col, row - 1) from (col, row).
        for (row in 0..rows) {
            val y = row.toFloat() / rows
            var runStart = -1
            for (col in 0..cols) {
                val drawn = col < cols && drawsEdge(col, row - 1, col, row)
                if (drawn && runStart < 0) runStart = col
                if (!drawn && runStart >= 0) {
                    lines += GridLine(runStart.toFloat() / cols, y, col.toFloat() / cols, y)
                    runStart = -1
                }
            }
        }
        return lines
    }

    /**
     * Pixel rect to fill for [line] on a [canvasWidthPx] x [canvasHeightPx] surface with a
     * [strokePx]-wide pen.
     *
     * A filled rect rather than a stroked path so the two renderers cannot end up with
     * different cap or join behaviour, and so crossings are solid: each segment is
     * extended by half a stroke at both ends, which fills the square where a vertical and
     * a horizontal meet.
     *
     * The line is centred on the boundary it describes, then pushed inside the canvas at
     * the outer edges — a table's outermost lines sit exactly on 0 and 1, and a centred
     * pen there would draw half of itself off-surface, leaving the outside of the table
     * ruled half as thickly as its inside.
     */
    fun gridLineRect(
        line: GridLine,
        canvasWidthPx: Float,
        canvasHeightPx: Float,
        strokePx: Float,
    ): PadRect {
        val half = strokePx / 2f
        return if (line.isVertical) {
            val centre = (line.startX * canvasWidthPx).coerceIn(half, (canvasWidthPx - half).coerceAtLeast(half))
            val top = (line.startY * canvasHeightPx - half).coerceAtLeast(0f)
            val bottom = (line.endY * canvasHeightPx + half).coerceAtMost(canvasHeightPx)
            PadRect(
                left = centre - half,
                top = top,
                width = strokePx,
                height = (bottom - top).coerceAtLeast(0f),
            )
        } else {
            val centre = (line.startY * canvasHeightPx).coerceIn(half, (canvasHeightPx - half).coerceAtLeast(half))
            val left = (line.startX * canvasWidthPx - half).coerceAtLeast(0f)
            val right = (line.endX * canvasWidthPx + half).coerceAtMost(canvasWidthPx)
            PadRect(
                left = left,
                top = centre - half,
                width = (right - left).coerceAtLeast(0f),
                height = strokePx,
            )
        }
    }

    /**
     * Which corners of the face on cell [col], [row] are rounded.
     *
     * **A corner is rounded when both of the edges meeting at it are ruled, and square
     * otherwise.** It is not a fifth rule — it is [gridLines]' one rule read at a point
     * instead of along a line, which is why both go through the same [EdgeRuling]. Any
     * reason an edge goes unruled therefore reaches the corners for free: the grid switched
     * off ([PadLayout.gridShowBorders]), and two cells held by the same button, which is the
     * interior of a merged button and must read as one continuous face.
     *
     * The alternative was a table of "which corner in which situation", which would have to
     * be extended by hand every time the ruling learns a new reason to skip an edge — and
     * would be wrong, silently and only on screen, until someone noticed.
     *
     * [outlineEmptyCells] means the same thing here as it does in [gridLines], and must be
     * passed the same value by the same caller: a cell whose outline only exists because the
     * editor draws empty cells must round to that outline, not to the live pad's absent one.
     */
    fun cellCorners(
        layout: PadLayout,
        col: Int,
        row: Int,
        outlineEmptyCells: Boolean,
    ): CellCorners {
        if (!layout.isGridMode) return CellCorners.SQUARE
        val button = buttonAt(layout, col, row)
        val colSpan = button?.effectiveColSpan ?: 1
        val rowSpan = button?.effectiveRowSpan ?: 1
        val startCol = button?.gridCol ?: col
        val startRow = button?.gridRow ?: row

        val ruling = EdgeRuling(layout, outlineEmptyCells) { c, r -> buttonAt(layout, c, r)?.id }

        val top = ruling.isRuled(startCol, startRow - 1, startCol, startRow)
        val bottom = ruling.isRuled(startCol, startRow + rowSpan - 1, startCol, startRow + rowSpan)
        val left = ruling.isRuled(startCol - 1, startRow, startCol, startRow)
        val right = ruling.isRuled(startCol + colSpan - 1, startRow, startCol + colSpan, startRow)

        return CellCorners(
            topLeft = top && left,
            topRight = top && right,
            bottomRight = bottom && right,
            bottomLeft = bottom && left,
        )
    }

    /**
     * The four corner radii, in pixels, of the face on cell [col], [row].
     *
     * The single call both renderers make: [cellCorners] decides which corners are rounded
     * at all, [faceCornerRadiusPx] decides how far a face this size can be rounded, and the
     * two answers are combined here rather than at each call site. The live pad and the
     * editor differ only in how they measure the face and in [outlineEmptyCells]; if they
     * ever round a cell differently again it will be because they measured it differently,
     * not because one of them reasoned about corners on its own.
     *
     * The order matches Compose's `RoundedCornerShape(topStart, topEnd, bottomEnd,
     * bottomStart)` for left-to-right layouts, so a caller wraps these without reordering.
     * Table geometry is column-major and not mirrored for RTL — a table's first column is
     * its left column in every locale — so "start" is "left" here by construction.
     */
    fun cellFaceRadiiPx(
        layout: PadLayout,
        col: Int,
        row: Int,
        outlineEmptyCells: Boolean,
        faceWidthPx: Float,
        faceHeightPx: Float,
        requestedRadiusPx: Float,
    ): FaceCornerRadii {
        val radiusPx = faceCornerRadiusPx(faceWidthPx, faceHeightPx, requestedRadiusPx)
        if (radiusPx <= 0f) return FaceCornerRadii.SQUARE
        val corners = cellCorners(layout, col, row, outlineEmptyCells)
        return FaceCornerRadii(
            topLeftPx = if (corners.topLeft) radiusPx else 0f,
            topRightPx = if (corners.topRight) radiusPx else 0f,
            bottomRightPx = if (corners.bottomRight) radiusPx else 0f,
            bottomLeftPx = if (corners.bottomLeft) radiusPx else 0f,
        )
    }

    /**
     * The one decision "is this edge ruled?", shared by [gridLines] and [cellCorners].
     *
     * Exists so the two cannot drift: a corner is rounded precisely when the edges meeting
     * at it are drawn, and that is only true by construction if both questions are answered
     * by the same code. [occupantIdAt] is supplied by the caller because [gridLines] walks
     * the whole table and pre-reads occupancy into a grid, while [cellCorners] asks about at
     * most eight cells and would pay more to build that grid than to scan for them.
     * [occupantIdAt] is only ever called with in-range coordinates.
     */
    private class EdgeRuling(
        layout: PadLayout,
        private val outlineEmptyCells: Boolean,
        private val occupantIdAt: (col: Int, row: Int) -> String?,
    ) {
        private val cols = layout.effectiveGridCols
        private val rows = layout.effectiveGridRows
        private val showAll = layout.gridShowBorders

        private fun isInside(
            col: Int,
            row: Int,
        ): Boolean = col in 0 until cols && row in 0 until rows

        private fun idAt(
            col: Int,
            row: Int,
        ): String? = if (isInside(col, row)) occupantIdAt(col, row) else null

        /** Whether the edge between cell [aCol],[aRow] and cell [bCol],[bRow] is drawn. */
        fun isRuled(
            aCol: Int,
            aRow: Int,
            bCol: Int,
            bRow: Int,
        ): Boolean {
            val aIn = isInside(aCol, aRow)
            val bIn = isInside(bCol, bRow)
            if (!aIn && !bIn) return false
            if (aIn && bIn) {
                val a = idAt(aCol, aRow)
                // Same non-null button on both sides: this edge is interior to one merged
                // button, so it is never drawn whatever the setting says.
                if (a != null && a == idAt(bCol, bRow)) return false
            }
            if (showAll || outlineEmptyCells) return true
            return false
        }
    }

    /**
     * Pixel rect [button] occupies on a [canvasWidthPx] x [canvasHeightPx] surface.
     *
     * The one place that answers "how big is this button". The renderer draws it, the
     * hit-test engine tests against it, so what is visible and what is touchable are
     * the same rectangle by construction rather than by two call sites agreeing.
     *
     * A button carrying [PadButton.resolvedCell] — i.e. one resolved by [resolveButtons]
     * on a table — fills its cell edge to edge, and [PadButton.buttonSize] plus the
     * [PadAction.TrackpointMove] size multiplier are ignored: on a table the cell is the
     * size, which is also why the editor hides both settings there. The stored values are
     * only overridden, never consumed, so free placement gets them back untouched.
     *
     * Two neighbours therefore share a boundary rather than sitting a gap apart. The
     * boundary belongs to whichever of them the hit test happens to reach first — that is
     * a single pixel column, and [PadRect.contains] is inclusive for the reason given
     * there.
     *
     * @param buttonUnitPx [BUTTON_UNIT_DP] in pixels — the 1x1 free-placement unit.
     */
    fun buttonRect(
        button: PadButton,
        canvasWidthPx: Float,
        canvasHeightPx: Float,
        buttonUnitPx: Float,
    ): PadRect {
        val cell = button.resolvedCell
        if (cell != null) {
            return cellRect(cell, canvasWidthPx, canvasHeightPx)
        }

        val action = button.action
        val widthPx: Float
        val heightPx: Float
        if (action is PadAction.TrackpointMove) {
            widthPx = buttonUnitPx * action.size.multiplier
            heightPx = widthPx
        } else {
            widthPx = buttonUnitPx * button.buttonSize.cols
            heightPx = buttonUnitPx * button.buttonSize.rows
        }
        return PadRect(
            left = button.posX * canvasWidthPx - widthPx / 2f,
            top = button.posY * canvasHeightPx - heightPx / 2f,
            width = widthPx,
            height = heightPx,
        )
    }

    /**
     * [buttons] in the order a renderer must draw them: **reversed, so the head of the list
     * is painted last and therefore lands on top**.
     *
     * The list the editor shows is `layout.buttons` in stored order, first item at the top,
     * and the user reads that top item as the topmost layer. Painting the list front to back
     * gave the opposite — the row at the top of the list was drawn first and ended up
     * underneath everything below it.
     *
     * **The stored order is not touched.** `layout.buttons` is read for more than drawing:
     * drag-reorder rewrites it by index, export/import round-trips it, duplication and
     * copy-to-layout preserve it, and `withCellsAssigned` fills table cells by walking it.
     * Reversing the list itself would mean every one of those either agreeing to be
     * reversed too or silently disagreeing with the screen, so the reversal is applied at
     * the one moment it is about — putting pixels down — and nowhere else.
     *
     * Returns a view, not a copy: nothing here mutates, and a renderer walking it once per
     * frame should not allocate a second list to do so.
     *
     * Table layouts are unaffected in practice — one button per cell, so nothing overlaps —
     * but they go through the same call rather than round a branch, because "which button is
     * on top" must have one answer and not one answer per layout mode.
     */
    fun paintOrder(buttons: List<PadButton>): List<PadButton> = buttons.asReversed()

    /**
     * The button at ([px], [py]), or null when the point is on none of them.
     *
     * **The other half of [paintOrder], and the reason both are here.** Where two buttons
     * overlap, the one that gets pressed has to be the one the user can see, so this returns
     * the *first* entry of [buttons] that contains the point — precisely the entry
     * [paintOrder] puts last, i.e. paints on top. The two are inverses of one another by
     * construction; split across a renderer and a hit-test engine they would be two
     * independent claims about z-order, which is the shape of the bug this pair replaces
     * (see `FIX_LOGS.md` `[表格按鈕沒撐滿格子]`, where the drawn and the touchable rect were
     * derived twice and drifted).
     *
     * Callers filter [buttons] first if a mode hides some of them — ambient peek does — and
     * must hand the *same* filtered list to both this and [paintOrder], for the same reason
     * `outlineEmptyCells` has to match between [gridLines] and [cellCorners].
     *
     * @param buttonUnitPx [BUTTON_UNIT_DP] in pixels, as [buttonRect] takes it.
     */
    fun topmostAt(
        buttons: List<PadButton>,
        px: Float,
        py: Float,
        canvasWidthPx: Float,
        canvasHeightPx: Float,
        buttonUnitPx: Float,
    ): PadButton? =
        buttons.firstOrNull { button ->
            buttonRect(button, canvasWidthPx, canvasHeightPx, buttonUnitPx).contains(px, py)
        }

    /**
     * Calculates the physical aspect ratio of a button cell or spanned cell block
     * in a table grid on the secondary screen.
     */
    fun cellAspectRatio(
        cols: Int,
        rows: Int,
        colSpan: Int = 1,
        rowSpan: Int = 1,
        containerAspect: Float = 4f / 3f,
    ): Float {
        val safeCols = cols.coerceAtLeast(1).toFloat()
        val safeRows = rows.coerceAtLeast(1).toFloat()
        val safeColSpan = colSpan.coerceAtLeast(1).toFloat()
        val safeRowSpan = rowSpan.coerceAtLeast(1).toFloat()
        val cellAspect = containerAspect * (safeRows / safeCols)
        return (cellAspect * (safeColSpan / safeRowSpan)).coerceAtLeast(0.1f)
    }
}

/** Pixel rect of a pad button face or a table cell frame. */
data class PadRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height

    /**
     * Whether ([x], [y]) falls on this rect, edges included.
     *
     * Inclusive because a button's own edge belongs to the button: on a table two cells
     * now share their boundary exactly, and an exclusive test would turn every shared
     * boundary into a dead pixel column.
     */
    fun contains(
        x: Float,
        y: Float,
    ): Boolean = x >= left && x <= right && y >= top && y <= bottom
}

/**
 * One straight grid line, in normalised 0..1 canvas coordinates.
 *
 * Always axis-aligned, and always stored with `start <= end` on the axis it runs along,
 * so a segment can be compared for equality without normalising it first.
 */
data class GridLine(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
) {
    /** True for a line running top-to-bottom, false for one running left-to-right. */
    val isVertical: Boolean get() = startX == endX
}

/**
 * Which corners of one table cell's face are rounded.
 *
 * Named for the screen, not for the writing direction: table columns are never mirrored,
 * so [topLeft] is the top-left corner in every locale.
 */
data class CellCorners(
    val topLeft: Boolean,
    val topRight: Boolean,
    val bottomRight: Boolean,
    val bottomLeft: Boolean,
) {
    companion object {
        /** No corner rounded — an unruled cell, and what a free layout gets. */
        val SQUARE = CellCorners(topLeft = false, topRight = false, bottomRight = false, bottomLeft = false)
    }
}

/**
 * Corner radii in pixels of one table cell's face, clockwise from the top-left.
 *
 * Ordered to match Compose's `RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)`
 * so neither renderer has to reorder them.
 */
data class FaceCornerRadii(
    val topLeftPx: Float,
    val topRightPx: Float,
    val bottomRightPx: Float,
    val bottomLeftPx: Float,
) {
    companion object {
        /** Every corner square. */
        val SQUARE = FaceCornerRadii(topLeftPx = 0f, topRightPx = 0f, bottomRightPx = 0f, bottomLeftPx = 0f)
    }
}

/** Normalised (0..1) edges of one table cell. */
data class CellBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/**
 * Gives every unplaced button a cell, filling row by row.
 *
 * Used when a layout is switched into table mode: without it, a layout that already
 * had buttons would show an empty table, because those buttons carry free positions
 * and no cell. Buttons that already hold a cell keep it, and once the table is full
 * the remainder are left unplaced — their data survives, and widening the table lets
 * a later pass place them.
 */
fun PadLayout.withCellsAssigned(): PadLayout {
    if (!isGridMode) return this

    val cols = effectiveGridCols
    val rows = effectiveGridRows
    val taken = mutableSetOf<Pair<Int, Int>>()
    for (button in buttons) {
        val c = button.gridCol ?: continue
        val r = button.gridRow ?: continue
        if (button.isWithinGrid(cols, rows)) {
            for (dc in 0 until button.effectiveColSpan) {
                for (dr in 0 until button.effectiveRowSpan) {
                    taken.add((c + dc) to (r + dr))
                }
            }
        }
    }

    val freeCells =
        sequence {
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    if ((col to row) !in taken) yield(col to row)
                }
            }
        }.iterator()

    val placed =
        buttons.map { button ->
            if (button.isWithinGrid(cols, rows)) {
                button
            } else if (freeCells.hasNext()) {
                val (col, row) = freeCells.next()
                button.copy(gridCol = col, gridRow = row)
            } else {
                button
            }
        }
    return copy(buttons = placed)
}

/**
 * How many buttons a table of [cols] x [rows] would be unable to show.
 *
 * Answers the question the grid-size control has to answer *before* the user commits:
 * shrinking a table hides the buttons that no longer have a cell — it never deletes them
 * (see [GridLayoutMath.resolveButtons]) — and a warning that cannot say how many buttons
 * are involved is not much of a warning.
 *
 * The count is taken **after** [withCellsAssigned], because that is what the commit path
 * (`withLayoutMode`) runs: growing a table hands the cells that appeared to buttons that
 * were previously unplaced, so counting the current `gridCol`/`gridRow` values alone
 * would over-report on every enlargement.
 *
 * Answers for the layout as a table regardless of its current [PadLayout.layoutMode], so
 * the same preview works from a free layout that is about to become one. Out-of-range
 * [cols] / [rows] are clamped exactly as [PadLayout.effectiveGridCols] clamps them.
 */
fun PadLayout.hiddenButtonCountForGrid(
    cols: Int,
    rows: Int,
): Int {
    val resized =
        copy(
            layoutMode = PadLayoutMode.GRID,
            gridCols = cols,
            gridRows = rows,
        ).withCellsAssigned()
    val safeCols = resized.effectiveGridCols
    val safeRows = resized.effectiveGridRows
    return resized.buttons.count { !it.isWithinGrid(safeCols, safeRows) }
}
