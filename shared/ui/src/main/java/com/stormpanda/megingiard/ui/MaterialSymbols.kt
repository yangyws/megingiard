package com.stormpanda.megingiard.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.stormpanda.megingiard.shared.ui.R

private const val TAG = "MaterialSymbols"

private const val MS_FILL_FILLED = 1f // 1 = filled (matches former Icons.Rounded look)
private const val MS_FILL_OUTLINE = 0f // 0 = outline
private const val MS_WEIGHT = 400
private const val MS_GRAD = 0f
private const val MS_OPT_SIZE = 24f
private const val MS_OPTICAL_Y_OFFSET_FACTOR = -0.06f

/** [FontFamily] backed by the bundled Material Symbols Rounded variable font — filled variant (FILL=1). */
@OptIn(ExperimentalTextApi::class)
val MaterialSymbolsFamily: FontFamily =
    FontFamily(
        Font(
            resId = R.font.material_symbols_rounded,
            weight = FontWeight.W400,
            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(MS_WEIGHT),
                    FontVariation.Setting("FILL", MS_FILL_FILLED),
                    FontVariation.Setting("GRAD", MS_GRAD),
                    FontVariation.Setting("opsz", MS_OPT_SIZE),
                ),
        ),
    )

/** [FontFamily] backed by the bundled Material Symbols Rounded variable font — outline variant (FILL=0). */
@OptIn(ExperimentalTextApi::class)
val MaterialSymbolsOutlineFamily: FontFamily =
    FontFamily(
        Font(
            resId = R.font.material_symbols_rounded,
            weight = FontWeight.W400,
            variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(MS_WEIGHT),
                    FontVariation.Setting("FILL", MS_FILL_OUTLINE),
                    FontVariation.Setting("GRAD", MS_GRAD),
                    FontVariation.Setting("opsz", MS_OPT_SIZE),
                ),
        ),
    )

private val MS_CENTERED_STYLE =
    TextStyle(
        platformStyle =
            @Suppress("DEPRECATION")
            PlatformTextStyle(
                includeFontPadding = false,
            ),
        lineHeightStyle =
            LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both,
            ),
    )

/**
 * Renders a single Material Symbol ligature by [name] (e.g. `"arrow_back"`) using
 * the bundled Material Symbols Rounded variable font at the given [size].
 *
 * @param filled `true` (default) renders the filled variant; `false` renders the outline variant.
 */
@Composable
fun MaterialSymbol(
    name: String,
    size: Dp,
    tint: Color,
    filled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val fontSize = with(LocalDensity.current) { size.toSp() }
    val verticalOffset = size * MS_OPTICAL_Y_OFFSET_FACTOR
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name,
            fontFamily = if (filled) MaterialSymbolsFamily else MaterialSymbolsOutlineFamily,
            fontSize = fontSize,
            color = tint,
            lineHeight = fontSize,
            textAlign = TextAlign.Center,
            style = MS_CENTERED_STYLE,
            maxLines = 1,
            modifier = Modifier.offset(y = verticalOffset),
        )
    }
}
