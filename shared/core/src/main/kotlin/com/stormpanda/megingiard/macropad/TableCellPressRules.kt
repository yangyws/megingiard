package com.stormpanda.megingiard.macropad

/**
 * How far a press on a table cell has progressed.
 *
 * Declaration order is chronological: a press enters [TAP], may reach [MENU], and may reach
 * [MOVE]. It never goes backwards — the phase is a function of elapsed hold time alone, so
 * nothing a finger does can un-arm it short of lifting.
 */
enum class TableCellPressPhase {
    /** Below the long-press threshold. A release here is an ordinary tap. */
    TAP,

    /**
     * Past the long-press threshold, short of the move threshold.
     */
    MENU,

    /**
     * Past the move threshold. The cell is picked up and movement drags it.
     */
    MOVE,
}

/** What a press on a table cell asks the editor to do once the finger lifts. */
enum class TableCellPressOutcome {
    /** Nothing at all. The press is discarded. */
    IGNORE,

    /** Open the cell: the button editor, on the tapped button or on a new one for an empty cell. */
    TAP_CELL,

    /** Open the edit/delete menu for the button in the pressed cell. */
    OPEN_MENU,

    /** Move the button from the pressed cell to the cell the finger ended over. */
    MOVE_CELL,
}

/**
 * The three-stage press a table cell answers to, as pure arithmetic over "how long has the
 * finger been down" and "how far has it moved".
 *
 * Pure functions with no Android dependency, so they are unit-testable without Robolectric.
 */
object TableCellPressRules {
    /**
     * Extra hold time, on top of the platform long-press timeout, before a cell is picked up.
     */
    const val MOVE_ARM_EXTRA_MILLIS: Long = 0L

    /**
     * Hold duration, from the initial press, at which the cell is picked up.
     *
     * @param longPressTimeoutMillis the platform long-press timeout — pass
     *   `ViewConfiguration.longPressTimeoutMillis`, never a hard-coded 500.
     */
    fun moveArmMillis(longPressTimeoutMillis: Long): Long = longPressTimeoutMillis + MOVE_ARM_EXTRA_MILLIS

    /**
     * The phase a press is in after being held for [heldMillis].
     */
    fun phaseAt(
        heldMillis: Long,
        longPressTimeoutMillis: Long,
    ): TableCellPressPhase =
        when {
            heldMillis < longPressTimeoutMillis -> TableCellPressPhase.TAP
            else -> TableCellPressPhase.MOVE
        }

    /**
     * Whether the pressed cell draws its "picked up" frame.
     */
    fun showsMoveArmedFrame(phase: TableCellPressPhase): Boolean = phase == TableCellPressPhase.MOVE

    /**
     * Whether finger movement should be tracked as a drag — following the drop target and
     * consuming the pointer.
     */
    fun tracksDragTarget(
        phase: TableCellPressPhase,
        movedPastSlop: Boolean,
    ): Boolean = phase == TableCellPressPhase.MOVE && movedPastSlop

    /**
     * What lifting the finger should do.
     *
     * Tapping opens the edit dialog directly. Long pressing enters move mode directly.
     * Releasing without dragging cancels the move without opening any selection menu.
     */
    fun outcomeOnRelease(
        phase: TableCellPressPhase,
        slidBeforeLongPress: Boolean,
        movedAfterPickUp: Boolean,
        cellHasButton: Boolean,
    ): TableCellPressOutcome =
        when (phase) {
            TableCellPressPhase.TAP ->
                if (slidBeforeLongPress) TableCellPressOutcome.IGNORE else TableCellPressOutcome.TAP_CELL

            TableCellPressPhase.MENU, TableCellPressPhase.MOVE ->
                if (cellHasButton && movedAfterPickUp) {
                    TableCellPressOutcome.MOVE_CELL
                } else {
                    TableCellPressOutcome.IGNORE
                }
        }
}
