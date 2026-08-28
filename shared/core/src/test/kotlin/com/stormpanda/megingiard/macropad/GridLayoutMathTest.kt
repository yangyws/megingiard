package com.stormpanda.megingiard.macropad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun testCalculateExpandedGridColRightThenLeft() {
        // In a 4-col grid:
        // Button at col=2, target span=2 -> fits right (col 2, 3) -> anchor remains 2
        assertEquals(2, GridLayoutMath.calculateExpandedGridCol(4, 2, 2))
        // Button at col=2, target span=3 -> right boundary overflow (2+3=5>4) -> shifts left to col 1 (span 1, 2, 3)
        assertEquals(1, GridLayoutMath.calculateExpandedGridCol(4, 2, 3))
        // Button at col=3, target span=2 -> shifts left to col 2 (span 2, 3)
        assertEquals(2, GridLayoutMath.calculateExpandedGridCol(4, 3, 2))
        // Button at col=3, target span=4 -> shifts left to col 0 (span 0, 1, 2, 3)
        assertEquals(0, GridLayoutMath.calculateExpandedGridCol(4, 3, 4))
    }

    @Test
    fun testCalculateExpandedGridRowDownThenUp() {
        // In a 3-row grid:
        // Button at row=1, target span=2 -> fits down (row 1, 2) -> anchor remains 1
        assertEquals(1, GridLayoutMath.calculateExpandedGridRow(3, 1, 2))
        // Button at row=1, target span=3 -> bottom boundary overflow (1+3=4>3) -> shifts up to row 0 (span 0, 1, 2)
        assertEquals(0, GridLayoutMath.calculateExpandedGridRow(3, 1, 3))
        // Button at row=2, target span=2 -> shifts up to row 1 (span 1, 2)
        assertEquals(1, GridLayoutMath.calculateExpandedGridRow(3, 2, 2))
    }
}


