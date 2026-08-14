package com.stormpanda.megingiard.macropad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stormpanda.megingiard.R
import com.stormpanda.megingiard.ui.AppModalDialog
import com.stormpanda.megingiard.ui.LocalAppColors
import java.util.Locale

private const val TAG = "InlineGridSizeOverlay"

private const val IGSO_WIDTH_FRACTION = 0.85f
private val IGSO_PADDING = 16.dp
private val IGSO_ROW_GAP = 12.dp
private val IGSO_STEPPER_BUTTON_SIZE = 40.dp
private val IGSO_STEPPER_CORNER = 8.dp
private val IGSO_VALUE_TEXT_WIDTH = 48.dp

private const val BOTTOM_PANEL_DIAGONAL_INCH = 3.92f
private const val MM_PER_INCH = 25.4f
private const val GRID_HINT_PADDING_DP = 24
private const val GRID_MIN_COMFORTABLE_MM = 9f

@Composable
internal fun InlineGridSizeOverlay(
    layout: PadLayout,
    accentColor: Color,
    onConfirm: (cols: Int, rows: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    var cols by remember(layout.id) { mutableIntStateOf(layout.effectiveGridCols) }
    var rows by remember(layout.id) { mutableIntStateOf(layout.effectiveGridRows) }

    val hiddenCount = remember(layout, cols, rows) { layout.hiddenButtonCountForGrid(cols, rows) }

    AppModalDialog(
        onDismiss = onDismiss,
        widthFraction = IGSO_WIDTH_FRACTION,
        cornerRadius = 12.dp,
        contentPadding = IGSO_PADDING,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.macropad_editor_cancel),
                    color = colors.onSurfaceSecondary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                text = stringResource(R.string.macropad_editor_grid_size),
                color = colors.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = { onConfirm(cols, rows) }) {
                Text(
                    text = stringResource(R.string.macropad_editor_done),
                    color = accentColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(IGSO_ROW_GAP))

        GridSizeSettingRow(
            label = stringResource(R.string.layout_settings_grid_cols),
            value = cols,
            max = MAX_GRID_COLS,
            accentColor = accentColor,
            onValueChange = { cols = it },
        )

        Spacer(Modifier.height(IGSO_ROW_GAP))

        GridSizeSettingRow(
            label = stringResource(R.string.layout_settings_grid_rows),
            value = rows,
            max = MAX_GRID_ROWS,
            accentColor = accentColor,
            onValueChange = { rows = it },
        )

        Spacer(Modifier.height(IGSO_ROW_GAP))

        GridCellSizeHint(cols = cols, rows = rows)

        if (hiddenCount > 0) {
            Spacer(Modifier.height(IGSO_ROW_GAP))
            Text(
                text = stringResource(R.string.macropad_editor_grid_size_hidden_hint, hiddenCount),
                color = colors.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun GridSizeSettingRow(
    label: String,
    value: Int,
    max: Int,
    accentColor: Color,
    onValueChange: (Int) -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = colors.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        GridStepperButton(
            symbol = "−",
            enabled = value > MIN_GRID_SIZE,
            accentColor = accentColor,
            onClick = { onValueChange((value - 1).coerceAtLeast(MIN_GRID_SIZE)) },
        )
        Text(
            text = value.toString(),
            color = colors.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(IGSO_VALUE_TEXT_WIDTH),
        )
        GridStepperButton(
            symbol = "+",
            enabled = value < max,
            accentColor = accentColor,
            onClick = { onValueChange((value + 1).coerceAtMost(max)) },
        )
    }
}

@Composable
private fun GridStepperButton(
    symbol: String,
    enabled: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    Box(
        modifier =
            Modifier
                .size(IGSO_STEPPER_BUTTON_SIZE)
                .clip(RoundedCornerShape(IGSO_STEPPER_CORNER))
                .background(if (enabled) accentColor.copy(alpha = 0.18f) else colors.surface)
                .border(
                    width = 1.dp,
                    color = if (enabled) accentColor.copy(alpha = 0.6f) else colors.navQuickMenuBorder,
                    shape = RoundedCornerShape(IGSO_STEPPER_CORNER),
                ).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            color = if (enabled) colors.onSurface else colors.onSurfaceSecondary,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun GridCellSizeHint(
    cols: Int,
    rows: Int,
) {
    val colors = LocalAppColors.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val cellWidthDp = ((configuration.screenWidthDp - GRID_HINT_PADDING_DP) / cols.coerceAtLeast(1))
    val cellHeightDp = ((configuration.screenHeightDp - GRID_HINT_PADDING_DP) / rows.coerceAtLeast(1))

    val mmPerDp = rememberMmPerDp(density.density)
    val widthMm = cellWidthDp * mmPerDp
    val heightMm = cellHeightDp * mmPerDp
    val tooSmall = minOf(widthMm, heightMm) < GRID_MIN_COMFORTABLE_MM

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text =
                stringResource(
                    R.string.layout_settings_grid_cell_size,
                    cellWidthDp,
                    cellHeightDp,
                    String.format(Locale.US, "%.1f", widthMm),
                    String.format(Locale.US, "%.1f", heightMm),
                ),
            color = if (tooSmall) colors.error else colors.onSurfaceSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        if (tooSmall) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.layout_settings_grid_cell_too_small),
                color = colors.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun rememberMmPerDp(density: Float): Float {
    val configuration = LocalConfiguration.current
    return remember(density, configuration.screenWidthDp, configuration.screenHeightDp) {
        val widthPx = configuration.screenWidthDp * density
        val heightPx = configuration.screenHeightDp * density
        val diagonalPx = kotlin.math.hypot(widthPx, heightPx)
        val realDpi = diagonalPx / BOTTOM_PANEL_DIAGONAL_INCH
        if (realDpi <= 0f) 0f else MM_PER_INCH * density / realDpi
    }
}
