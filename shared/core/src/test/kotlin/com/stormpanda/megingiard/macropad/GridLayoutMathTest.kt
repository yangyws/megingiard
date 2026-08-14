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
}
