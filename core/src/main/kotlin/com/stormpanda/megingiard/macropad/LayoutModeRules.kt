package com.stormpanda.megingiard.macropad

/**
 * Single source of truth for moving a [PadLayout] between [PadLayoutMode] values.
 *
 * The mode used to be reachable from exactly one place — the layout settings editor —
 * so the switch and the follow-up work it needs ([withCellsAssigned]) lived inline at
 * that one call site. It is now also reachable from the editor toolbar, and a second
 * hand-written copy of "set the mode, then place the buttons" is precisely the shape
 * that produced `[投影定格模式]` in FIX_LOGS: several call sites each spelling out the
 * same rule, one of them eventually missing a step.
 *
 * Everything here is **additive**. Switching modes never clears a field:
 *
 * - `gridCol` / `gridRow` survive a trip through [PadLayoutMode.FREE], so going back to
 *   a table restores the arrangement instead of re-filling it row by row.
 * - `posX` / `posY` survive a trip through [PadLayoutMode.GRID] — the cell centres a
 *   table renders at are stamped on by [GridLayoutMath.resolveButtons] for that render
 *   pass only and never written back.
 * - `mirrorCutouts` survive a switch into [PadLayoutMode.GRID] even though a table does
 *   not project (see `LayoutMirrorRules.activeMirrorCutouts`). They stop being rendered;
 *   they are not deleted.
 *
 * Pure functions with no Android dependency, so they are unit-testable without
 * Robolectric.
 */

/**
 * The other [PadLayoutMode].
 *
 * Exhaustive `when` rather than index arithmetic over `entries`: a third mode must be an
 * explicit decision here, not silently become "the next one in declaration order".
 */
fun PadLayoutMode.toggled(): PadLayoutMode =
    when (this) {
        PadLayoutMode.FREE -> PadLayoutMode.GRID
        PadLayoutMode.GRID -> PadLayoutMode.FREE
    }

/**
 * This layout in [mode], with the invariant that mode requires already applied.
 *
 * A layout in [PadLayoutMode.GRID] always comes back through [withCellsAssigned], so
 * every button that can hold a cell holds one. That covers both callers with a single
 * rule:
 *
 * - the toolbar toggle, where a free layout would otherwise arrive as an empty table
 *   because its buttons carry positions and no cell;
 * - the layout settings editor, where `gridCols` / `gridRows` may have grown in the same
 *   edit and the cells that appeared need filling, even though the mode did not change.
 *
 * Switching **into** [PadLayoutMode.FREE] is a plain mode change: every button still
 * carries the free position it was created with, and its cell is left in place for the
 * next switch back. A free layout asked for [PadLayoutMode.FREE] is returned as-is, so a
 * no-op does not look like an edit.
 */
fun PadLayout.withLayoutMode(mode: PadLayoutMode): PadLayout {
    val switched = if (layoutMode == mode) this else copy(layoutMode = mode)
    return if (switched.isGridMode) switched.withCellsAssigned() else switched
}

/** This layout in the mode it is not currently in. See [withLayoutMode]. */
fun PadLayout.withLayoutModeToggled(): PadLayout = withLayoutMode(layoutMode.toggled())
