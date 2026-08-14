package com.stormpanda.megingiard.macropad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TableCellPressRulesTest {
    private val longPress = 500L

    @Test
    fun testMoveThresholdEqualsLongPress() {
        assertEquals(longPress, TableCellPressRules.moveArmMillis(longPress))
        assertEquals(350L, TableCellPressRules.moveArmMillis(350L))
        assertEquals(650L, TableCellPressRules.moveArmMillis(650L))
    }

    @Test
    fun testMoveArmExtraMillisIsZero() {
        assertEquals(0L, TableCellPressRules.MOVE_ARM_EXTRA_MILLIS)
        assertTrue(TableCellPressRules.moveArmMillis(longPress) < 1000L)
    }

    @Test
    fun testPhaseTransitions() {
        assertEquals(TableCellPressPhase.TAP, TableCellPressRules.phaseAt(0L, longPress))
        assertEquals(TableCellPressPhase.TAP, TableCellPressRules.phaseAt(longPress - 1, longPress))
        assertEquals(TableCellPressPhase.MOVE, TableCellPressRules.phaseAt(longPress, longPress))
        assertEquals(TableCellPressPhase.MOVE, TableCellPressRules.phaseAt(60_000L, longPress))
    }

    @Test
    fun testEveryDurationResolvesToPhase() {
        var previous = TableCellPressPhase.TAP
        for (held in 0L..1200L step 5L) {
            val phase = TableCellPressRules.phaseAt(held, longPress)
            assertTrue(
                "phase went backwards at ${held}ms: $previous -> $phase",
                phase.ordinal >= previous.ordinal,
            )
            previous = phase
        }
        assertEquals(TableCellPressPhase.MOVE, previous)
    }

    @Test
    fun testPickedUpFrame() {
        assertTrue(TableCellPressRules.showsMoveArmedFrame(TableCellPressRules.phaseAt(longPress, longPress)))
        assertFalse(TableCellPressRules.showsMoveArmedFrame(TableCellPressPhase.TAP))
        assertTrue(TableCellPressRules.showsMoveArmedFrame(TableCellPressPhase.MOVE))
    }

    @Test
    fun testDragTracking() {
        for (phase in TableCellPressPhase.entries) {
            for (moved in listOf(false, true)) {
                val expected = phase == TableCellPressPhase.MOVE && moved
                assertEquals(
                    "phase=$phase moved=$moved",
                    expected,
                    TableCellPressRules.tracksDragTarget(phase, moved),
                )
            }
        }
    }

    @Test
    fun testReleaseOutcomes() {
        assertEquals(
            TableCellPressOutcome.TAP_CELL,
            outcome(TableCellPressPhase.TAP, cellHasButton = true),
        )
        assertEquals(
            TableCellPressOutcome.TAP_CELL,
            outcome(TableCellPressPhase.TAP, cellHasButton = false),
        )
        assertEquals(
            TableCellPressOutcome.IGNORE,
            outcome(TableCellPressPhase.TAP, slid = true, cellHasButton = true),
        )
        assertEquals(
            TableCellPressOutcome.IGNORE,
            outcome(TableCellPressPhase.MOVE, moved = false, cellHasButton = true),
        )
        assertEquals(
            TableCellPressOutcome.MOVE_CELL,
            outcome(TableCellPressPhase.MOVE, moved = true, cellHasButton = true),
        )
    }

    private fun outcome(
        phase: TableCellPressPhase,
        slid: Boolean = false,
        moved: Boolean = false,
        cellHasButton: Boolean,
    ): TableCellPressOutcome = TableCellPressRules.outcomeOnRelease(phase, slid, moved, cellHasButton)
}
