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

    @Test
    fun testMoveMultiCellButtonRightToEmptyCell() {
        // btn1 is 2x1 at (0, 0) (covers (0,0) and (1,0))
        val btn1 = createButton("btn1", 0, 0).copy(colSpan = 2, rowSpan = 1)
        val layout =
            PadLayout(
                id = "layout1",
                name = "Test",
                layoutMode = PadLayoutMode.GRID,
                buttons = listOf(btn1),
                gridCols = 4,
                gridRows = 2,
            )

        // Move btn1 right by 1 cell (from (0,0) to (1,0))
        val moved = GridLayoutMath.swapOrMoveButton(layout, Pair(0, 0), Pair(1, 0))
        val movedBtn1 = moved.buttons.firstOrNull { it.id == "btn1" }
        assertNotNull(movedBtn1)
        assertEquals(1, movedBtn1?.gridCol)
        assertEquals(0, movedBtn1?.gridRow)
    }

    @Test
    fun testMoveMultiCellButtonRightSwappingWithDisplacedButton() {
        // btn1 is 2x1 at (0, 0), btn2 is 1x1 at (2, 0)
        val btn1 = createButton("btn1", 0, 0).copy(colSpan = 2, rowSpan = 1)
        val btn2 = createButton("btn2", 2, 0)
        val layout =
            PadLayout(
                id = "layout1",
                name = "Test",
                layoutMode = PadLayoutMode.GRID,
                buttons = listOf(btn1, btn2),
                gridCols = 4,
                gridRows = 2,
            )

        // Move btn1 right by 1 cell: btn1 occupies (1,0)..(2,0), displacing btn2 to (0, 0)
        val moved = GridLayoutMath.swapOrMoveButton(layout, Pair(0, 0), Pair(1, 0))
        val movedBtn1 = moved.buttons.firstOrNull { it.id == "btn1" }
        val movedBtn2 = moved.buttons.firstOrNull { it.id == "btn2" }
        assertNotNull(movedBtn1)
        assertNotNull(movedBtn2)
        assertEquals(1, movedBtn1?.gridCol)
        assertEquals(0, movedBtn1?.gridRow)
        assertEquals(0, movedBtn2?.gridCol)
        assertEquals(0, movedBtn2?.gridRow)
    }

    @Test
    fun testMoveMultiCellButtonLeftSwappingWithDisplacedButton() {
        // btn1 is 2x1 at (1, 0) (covers 1..2), btn2 is 1x1 at (0, 0)
        val btn1 = createButton("btn1", 1, 0).copy(colSpan = 2, rowSpan = 1)
        val btn2 = createButton("btn2", 0, 0)
        val layout =
            PadLayout(
                id = "layout1",
                name = "Test",
                layoutMode = PadLayoutMode.GRID,
                buttons = listOf(btn1, btn2),
                gridCols = 4,
                gridRows = 2,
            )

        // Move btn1 left by 1 cell: btn1 occupies (0,0)..(1,0), displacing btn2 to (2, 0)
        val moved = GridLayoutMath.swapOrMoveButton(layout, Pair(1, 0), Pair(0, 0))
        val movedBtn1 = moved.buttons.firstOrNull { it.id == "btn1" }
        val movedBtn2 = moved.buttons.firstOrNull { it.id == "btn2" }
        assertNotNull(movedBtn1)
        assertNotNull(movedBtn2)
        assertEquals(0, movedBtn1?.gridCol)
        assertEquals(0, movedBtn1?.gridRow)
        assertEquals(2, movedBtn2?.gridCol)
        assertEquals(0, movedBtn2?.gridRow)
    }

    @Test
    fun testMoveMultiCellButtonVerticalSwapping() {
        // btn1 is 1x2 at (0, 0) (covers rows 0..1), btn2 is 1x1 at (0, 2)
        val btn1 = createButton("btn1", 0, 0).copy(colSpan = 1, rowSpan = 2)
        val btn2 = createButton("btn2", 0, 2)
        val layout =
            PadLayout(
                id = "layout1",
                name = "Test",
                layoutMode = PadLayoutMode.GRID,
                buttons = listOf(btn1, btn2),
                gridCols = 2,
                gridRows = 4,
            )

        // Move btn1 down by 1 cell: btn1 occupies (0, 1)..(0, 2), displacing btn2 to (0, 0)
        val movedDown = GridLayoutMath.swapOrMoveButton(layout, Pair(0, 0), Pair(0, 1))
        val downBtn1 = movedDown.buttons.firstOrNull { it.id == "btn1" }
        val downBtn2 = movedDown.buttons.firstOrNull { it.id == "btn2" }
        assertEquals(1, downBtn1?.gridRow)
        assertEquals(0, downBtn2?.gridRow)

        // Move btn1 back up from (0, 1) to (0, 0): displacing btn2 from (0, 0) to (0, 2)
        val movedUp = GridLayoutMath.swapOrMoveButton(movedDown, Pair(0, 1), Pair(0, 0))
        val upBtn1 = movedUp.buttons.firstOrNull { it.id == "btn1" }
        val upBtn2 = movedUp.buttons.firstOrNull { it.id == "btn2" }
        assertEquals(0, upBtn1?.gridRow)
        assertEquals(2, upBtn2?.gridRow)
    }

    @Test
    fun testDirectDragSwapOverDistance() {
        // btn1 is 1x1 at (0, 0), btn2 is 1x1 at (2, 2)
        val btn1 = createButton("btn1", 0, 0)
        val btn2 = createButton("btn2", 2, 2)
        val layout =
            PadLayout(
                id = "layout1",
                name = "Test",
                layoutMode = PadLayoutMode.GRID,
                buttons = listOf(btn1, btn2),
                gridCols = 4,
                gridRows = 4,
            )

        val swapped = GridLayoutMath.swapOrMoveButton(layout, Pair(0, 0), Pair(2, 2))
        val movedBtn1 = swapped.buttons.firstOrNull { it.id == "btn1" }
        val movedBtn2 = swapped.buttons.firstOrNull { it.id == "btn2" }
        assertEquals(2, movedBtn1?.gridCol)
        assertEquals(2, movedBtn1?.gridRow)
        assertEquals(0, movedBtn2?.gridCol)
        assertEquals(0, movedBtn2?.gridRow)
    }

    @Test
    fun testMoveMultiCellButtonBlockedWhenCollidingWithMultipleButtons() {
        // btn1 is 2x1 at (0, 0), btn2 is 1x1 at (2, 0), btn3 is 1x1 at (3, 0)
        val btn1 = createButton("btn1", 0, 0).copy(colSpan = 2, rowSpan = 1)
        val btn2 = createButton("btn2", 2, 0)
        val btn3 = createButton("btn3", 3, 0)
        val layout =
            PadLayout(
                id = "layout1",
                name = "Test",
                layoutMode = PadLayoutMode.GRID,
                buttons = listOf(btn1, btn2, btn3),
                gridCols = 4,
                gridRows = 2,
            )

        // Moving btn1 from (0, 0) to (2, 0) collides with BOTH btn2 and btn3 -> blocked in pure swap mode
        val moved = GridLayoutMath.swapOrMoveButton(layout, Pair(0, 0), Pair(2, 0))
        val movedBtn1 = moved.buttons.firstOrNull { it.id == "btn1" }
        // btn1 remains at (0, 0) because multi-button pushing is disabled
        assertEquals(0, movedBtn1?.gridCol)
    }

    @Test
    fun testMoveMultiCellButtonBlockedWhenSwapCausesOverlap() {
        // btn1 is 2x1 at (0, 0), btn2 is 2x1 at (2, 0)
        val btn1 = createButton("btn1", 0, 0).copy(colSpan = 2, rowSpan = 1)
        val btn2 = createButton("btn2", 2, 0).copy(colSpan = 2, rowSpan = 1)
        val layout =
            PadLayout(
                id = "layout1",
                name = "Test",
                layoutMode = PadLayoutMode.GRID,
                buttons = listOf(btn1, btn2),
                gridCols = 4,
                gridRows = 2,
            )

        // Moving btn1 by 1 cell right to (1, 0) collides with btn2.
        // Swapping anchors (btn1 at 1..2, btn2 at 0..1) causes overlap at col 1 -> blocked
        val moved = GridLayoutMath.swapOrMoveButton(layout, Pair(0, 0), Pair(1, 0))
        val movedBtn1 = moved.buttons.firstOrNull { it.id == "btn1" }
        assertEquals(0, movedBtn1?.gridCol)
    }

    @Test
    fun testCellAspectRatio() {
        // 4 cols x 3 rows on 4:3 screen:
        // cellAspect = (4/3) * (3/4) = 1.0f (exact square cell)
        val aspect1x1 = GridLayoutMath.cellAspectRatio(cols = 4, rows = 3, colSpan = 1, rowSpan = 1, containerAspect = 4f / 3f)
        assertEquals(1.0f, aspect1x1, 0.001f)

        // 2x1 cell in 4x3 grid:
        val aspect2x1 = GridLayoutMath.cellAspectRatio(cols = 4, rows = 3, colSpan = 2, rowSpan = 1, containerAspect = 4f / 3f)
        assertEquals(2.0f, aspect2x1, 0.001f)

        // 1x2 cell in 4x3 grid:
        val aspect1x2 = GridLayoutMath.cellAspectRatio(cols = 4, rows = 3, colSpan = 1, rowSpan = 2, containerAspect = 4f / 3f)
        assertEquals(0.5f, aspect1x2, 0.001f)

        // 8 cols x 2 rows on 4:3 screen:
        // cellAspect = (4/3) * (2/8) = (4/3) * (1/4) = 1/3 ~ 0.3333f
        val aspect8x2 = GridLayoutMath.cellAspectRatio(cols = 8, rows = 2, colSpan = 1, rowSpan = 1, containerAspect = 4f / 3f)
        assertEquals(1f / 3f, aspect8x2, 0.001f)
    }

    @Test
    fun testFindNeighborButton() {
        val btnTopLeft = createButton("tl", 0, 0)
        val btnTopRight = createButton("tr", 2, 0)
        val btnBottomLeft = createButton("bl", 0, 1)
        val layout =
            PadLayout(
                id = "layout1",
                name = "Test Layout",
                layoutMode = PadLayoutMode.GRID,
                buttons = listOf(btnTopLeft, btnTopRight, btnBottomLeft),
                gridCols = 4,
                gridRows = 3,
            )

        // Right from (0,0) jumps gap at col 1 and finds (2,0)
        val neighborRight = GridLayoutMath.findNeighborButton(layout, btnTopLeft, dirX = 1, dirY = 0)
        assertEquals("tr", neighborRight?.id)

        // Left from (2,0) finds (0,0)
        val neighborLeft = GridLayoutMath.findNeighborButton(layout, btnTopRight, dirX = -1, dirY = 0)
        assertEquals("tl", neighborLeft?.id)

        // Down from (0,0) finds (0,1)
        val neighborDown = GridLayoutMath.findNeighborButton(layout, btnTopLeft, dirX = 0, dirY = 1)
        assertEquals("bl", neighborDown?.id)

        // Up from (0,1) finds (0,0)
        val neighborUp = GridLayoutMath.findNeighborButton(layout, btnBottomLeft, dirX = 0, dirY = -1)
        assertEquals("tl", neighborUp?.id)

        // Up from (0,0) is null
        assertNull(GridLayoutMath.findNeighborButton(layout, btnTopLeft, dirX = 0, dirY = -1))
    }
}



