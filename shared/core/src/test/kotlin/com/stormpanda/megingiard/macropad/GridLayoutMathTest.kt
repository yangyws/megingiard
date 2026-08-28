package com.stormpanda.megingiard.macropad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GridLayoutMathTest {

    private fun createButton(id: String, col: Int, row: Int): PadButton {
        return PadButton(
            id = id,
            label = id,
            posX = 0.5f,
            posY = 0.5f,
            action = PadAction.KeyboardKey(keycode = 30, label = "A"),
            gridCol = col,
            gridRow = row,
        )
    }

    @Test
    fun testSwapButtons() {
        val btn1 = createButton("btn1", 0, 0)
        val btn2 = createButton("btn2", 1, 1)
        val layout =
            PadLayout(
                id = "layout1",
                name = "Test Layout",
                layoutMode = PadLayoutMode.GRID,
                buttons = listOf(btn1, btn2),
                gridCols = 4,
                gridRows = 3,
            )

        val swapped = GridLayoutMath.swapOrMoveButton(layout, Pair(0, 0), Pair(1, 1))

        val movedBtn1 = swapped.buttons.firstOrNull { it.id == "btn1" }
        val movedBtn2 = swapped.buttons.firstOrNull { it.id == "btn2" }

        assertNotNull(movedBtn1)
        assertNotNull(movedBtn2)
        assertEquals(1, movedBtn1?.gridCol)
        assertEquals(1, movedBtn1?.gridRow)
        assertEquals(0, movedBtn2?.gridCol)
        assertEquals(0, movedBtn2?.gridRow)
    }

    @Test
    fun testMoveToEmptyCell() {
        val btn1 = createButton("btn1", 0, 0)
        val layout =
            PadLayout(
                id = "layout1",
                name = "Test Layout",
                layoutMode = PadLayoutMode.GRID,
                buttons = listOf(btn1),
                gridCols = 4,
                gridRows = 3,
            )

        val moved = GridLayoutMath.swapOrMoveButton(layout, Pair(0, 0), Pair(2, 2))

        val movedBtn1 = moved.buttons.firstOrNull { it.id == "btn1" }
        assertNotNull(movedBtn1)
        assertEquals(2, movedBtn1?.gridCol)
        assertEquals(2, movedBtn1?.gridRow)
    }

    @Test
    fun testCanSpanButtonWithCollision() {
        val btn1 = createButton("btn1", 0, 0)
        val btn2 = createButton("btn2", 1, 0)
        val layout =
            PadLayout(
                id = "layout1",
                name = "Test Layout",
                layoutMode = PadLayoutMode.GRID,
                buttons = listOf(btn1, btn2),
                gridCols = 4,
                gridRows = 3,
            )

        // btn1 cannot expand horizontally because (1, 0) is occupied by btn2
        assertEquals(false, GridLayoutMath.canSpanButton(layout, btn1, 2, 1))
        // btn1 can expand vertically because (0, 1) is empty
        assertEquals(true, GridLayoutMath.canSpanButton(layout, btn1, 1, 2))
    }

    @Test
    fun testCanSpanButtonGridBounds() {
        val btn1 = createButton("btn1", 3, 2)
        val layout =
            PadLayout(
                id = "layout1",
                name = "Test Layout",
                layoutMode = PadLayoutMode.GRID,
                buttons = listOf(btn1),
                gridCols = 4,
                gridRows = 3,
            )

        // At (3, 2), expanding colSpan past 1 exceeds gridCols = 4 (3 + 2 = 5 > 4)
        assertEquals(false, GridLayoutMath.canSpanButton(layout, btn1, 2, 1))
        // At (3, 2), expanding rowSpan past 1 exceeds gridRows = 3 (2 + 2 = 4 > 3)
        assertEquals(false, GridLayoutMath.canSpanButton(layout, btn1, 1, 2))
    }

    @Test
    fun testGridShowButtonBgDefault() {
        val layout = PadLayout(id = "layout1", name = "Test Layout")
        assertEquals(true, layout.gridShowButtonBg)
        assertEquals(true, layout.gridShowBorders)

        val updated = layout.copy(gridShowButtonBg = false)
        assertEquals(false, updated.gridShowButtonBg)
    }

    @Test
    fun testGridLinesDrawnBetweenOccupiedCells() {
        val btn1 = createButton("btn1", 0, 0)
        val btn2 = createButton("btn2", 1, 0)
        val layout =
            PadLayout(
                id = "layout1",
                name = "Test Layout",
                layoutMode = PadLayoutMode.GRID,
                buttons = listOf(btn1, btn2),
                gridCols = 2,
                gridRows = 2,
                gridShowBorders = false,
            )

        val lines = GridLayoutMath.gridLines(layout, outlineEmptyCells = true)
        // With a 2x2 grid, all cell borders must be ruled: 3 vertical lines (col 0, 1, 2) and 3 horizontal lines (row 0, 1, 2)
        assertEquals(6, lines.size)
    }

    @Test
    fun testResizeOrSpanButtonShiftsCollidingButtons() {
        val btn1 = createButton("btn1", 0, 0)
        val btn2 = createButton("btn2", 1, 0)
        val layout =
            PadLayout(
                id = "layout1",
                name = "Test Layout",
                layoutMode = PadLayoutMode.GRID,
                buttons = listOf(btn1, btn2),
                gridCols = 2,
                gridRows = 2,
            )

        val expandedBtn1 = btn1.copy(colSpan = 2, rowSpan = 1)
        val result = GridLayoutMath.resizeOrSpanButton(layout, expandedBtn1)

        assertEquals(1, result.movedButtons.size)
        assertEquals(0, result.replacedButtons.size)
        assertEquals("btn2", result.movedButtons.first().id)
        // btn2 was shifted to an empty cell on row 1
        assertEquals(1, result.movedButtons.first().gridRow)
        assertEquals(2, result.layout.buttons.size)
    }

    @Test
    fun testResizeOrSpanButtonReplacesWhenGridIsFull() {
        val btn1 = createButton("btn1", 0, 0)
        val btn2 = createButton("btn2", 1, 0)
        val layout =
            PadLayout(
                id = "layout1",
                name = "Test Layout",
                layoutMode = PadLayoutMode.GRID,
                buttons = listOf(btn1, btn2),
                gridCols = 2,
                gridRows = 1,
            )

        val expandedBtn1 = btn1.copy(colSpan = 2, rowSpan = 1)
        val result = GridLayoutMath.resizeOrSpanButton(layout, expandedBtn1)

        assertEquals(0, result.movedButtons.size)
        assertEquals(1, result.replacedButtons.size)
        assertEquals("btn2", result.replacedButtons.first().id)
        assertEquals(1, result.layout.buttons.size)
        assertEquals("btn1", result.layout.buttons.first().id)
    }

    @Test
    fun testFindExpandedSpanColRightThenLeftWithoutDisplacingButtons() {
        // Grid: 4 cols x 2 rows
        // Cell (2, 0) has btn1. Cell (3, 0) has btn2.
        // Expanding btn1 to colSpan=2:
        // Right is blocked by btn2 at (3, 0).
        // Left has empty cell (1, 0), so it shifts left to col 1!
        val btn1 = createButton("btn1", 2, 0)
        val btn2 = createButton("btn2", 3, 0)
        val layout =
            PadLayout(
                id = "layout1",
                name = "Test",
                layoutMode = PadLayoutMode.GRID,
                buttons = listOf(btn1, btn2),
                gridCols = 4,
                gridRows = 2,
            )

        val expandedCol = GridLayoutMath.findExpandedSpanCol(layout, btn1, targetColSpan = 2)
        assertEquals(1, expandedCol)

        // If cell (1, 0) is ALSO occupied by btn3, then it cannot expand right OR left (no space for span 2 without collision)
        val btn3 = createButton("btn3", 1, 0)
        val fullLayout = layout.copy(buttons = listOf(btn1, btn2, btn3))
        val blockedCol = GridLayoutMath.findExpandedSpanCol(fullLayout, btn1, targetColSpan = 2)
        assertNull(blockedCol)
    }

    @Test
    fun testFindExpandedSpanRowDownThenUpWithoutDisplacingButtons() {
        // Grid: 2 cols x 3 rows
        // Cell (0, 1) has btn1. Cell (0, 2) has btn2.
        // Expanding btn1 to rowSpan=2:
        // Downwards is blocked by btn2 at (0, 2).
        // Upwards has empty cell (0, 0), so it shifts up to row 0!
        val btn1 = createButton("btn1", 0, 1)
        val btn2 = createButton("btn2", 0, 2)
        val layout =
            PadLayout(
                id = "layout1",
                name = "Test",
                layoutMode = PadLayoutMode.GRID,
                buttons = listOf(btn1, btn2),
                gridCols = 2,
                gridRows = 3,
            )

        val expandedRow = GridLayoutMath.findExpandedSpanRow(layout, btn1, targetRowSpan = 2)
        assertEquals(0, expandedRow)

        // If cell (0, 0) is ALSO occupied, then it cannot expand down OR up
        val btn3 = createButton("btn3", 0, 0)
        val fullLayout = layout.copy(buttons = listOf(btn1, btn2, btn3))
        val blockedRow = GridLayoutMath.findExpandedSpanRow(fullLayout, btn1, targetRowSpan = 2)
        assertNull(blockedRow)
    }
}


