package com.stormpanda.megingiard.macropad

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity

private const val PAD_GLYPH_LABEL_SCRIM_ALPHA = 0.45f

@Composable
internal fun PadButtonGlyph(
    btn: PadButton,
    size: Dp,
    tint: Color,
    modifier: Modifier = Modifier,
    labelOverlay: Boolean = true,
    faceSize: Dp? = null,
    faceReserve: Dp = 0.dp,
    isPressed: Boolean = false,
    accentColor: Color = tint,
    fallback: @Composable () -> Unit = {},
) {
    val assetId = btn.imageAssetId
    val iconName = btn.iconName

    if (assetId != null) {
        val context = LocalContext.current
        var image by remember(assetId) { mutableStateOf<ImageBitmap?>(null) }
        var missing by remember(assetId) { mutableStateOf(false) }

        LaunchedEffect(assetId) {
            val loaded = PadIconStore.load(context, assetId)
            if (loaded == null) missing = true else image = loaded.asImageBitmap()
        }

        val bitmap = image
        if (bitmap != null) {
            Box(
                modifier =
                    modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val scale = if (isPressed) 0.94f else 1.0f
                            scaleX = scale
                            scaleY = scale
                        },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Image(
                    bitmap = bitmap,
                    contentDescription = btn.label.ifBlank { null },
                    contentScale = if (btn.fullBleedIcon) ContentScale.FillBounds else ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (isPressed) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.22f)),
                    )
                }
                if (labelOverlay && btn.showLabel && btn.label.isNotBlank()) {
                    Text(
                        text = btn.label,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = PAD_GLYPH_LABEL_SCRIM_ALPHA))
                                .padding(horizontal = 2.dp, vertical = 1.dp),
                    )
                }
            }
            return
        }
        if (!missing) return
    }

    if (iconName != null) {
        if (btn.fullBleedIcon) {
            BoxWithConstraints(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                val density = LocalDensity.current
                val baseSize = 48.dp
                val basePx = with(density) { baseSize.toPx() }
                val targetW = constraints.maxWidth.toFloat()
                val targetH = constraints.maxHeight.toFloat()
                MaterialSymbol(
                    name = iconName,
                    size = baseSize,
                    tint = tint,
                    filled = btn.iconFilled,
                    modifier =
                        Modifier.graphicsLayer {
                            if (basePx > 0f && targetW > 0f && targetH > 0f) {
                                scaleX = targetW / basePx
                                scaleY = targetH / basePx
                            }
                        },
                )
            }
        } else {
            val glyphSize =
                PadGlyphRules
                    .glyphSizeDp(
                        defaultSizeDp = size.value,
                        faceSizeDp = faceSize?.value,
                        enlarge = btn.enlargeIcon,
                        fullBleed = false,
                        reserveDp = faceReserve.value,
                    ).dp
            MaterialSymbol(name = iconName, size = glyphSize, tint = tint, filled = btn.iconFilled, modifier = modifier)
        }
    } else if (btn.showLabel) {
        fallback()
    }
}
