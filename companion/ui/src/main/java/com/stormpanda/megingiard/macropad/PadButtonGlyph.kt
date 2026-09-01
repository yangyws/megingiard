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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val PAD_GLYPH_LABEL_SCRIM_ALPHA = 0.45f

@Composable
internal fun PadButtonGlyph(
    btn: PadButton,
    size: Dp,
    tint: Color,
    modifier: Modifier = Modifier,
    labelOverlay: Boolean = true,
    faceSize: Dp? = null,
    accentColor: Color = tint,
    isPressed: Boolean = false,
    isTableLayout: Boolean = false,
    width: Dp? = null,
    height: Dp? = null,
    fallback: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val imageAssetId = btn.imageAssetId
    val iconName = btn.iconName

    if (imageAssetId != null) {
        var bitmap by remember(imageAssetId) { mutableStateOf<ImageBitmap?>(null) }
        var missing by remember(imageAssetId) { mutableStateOf(false) }
        LaunchedEffect(imageAssetId) {
            val loaded = PadIconStore.load(context, imageAssetId)
            if (loaded != null) {
                bitmap = loaded.asImageBitmap()
                missing = false
            } else {
                bitmap = null
                missing = true
            }
        }
        if (bitmap != null) {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = bitmap!!,
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        val labelStyle =
                            if (btn.enlargeText) {
                                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            } else {
                                MaterialTheme.typography.labelSmall
                            }
                        Text(
                            text = btn.label,
                            color = Color.White,
                            style = labelStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (btn.showLabelBg) {
                                            Modifier.background(Color.Black.copy(alpha = PAD_GLYPH_LABEL_SCRIM_ALPHA))
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .padding(horizontal = 2.dp, vertical = 1.dp),
                        )
                    }
                }
            }
            return
        }
        if (!missing) return
    }

    if (iconName != null) {
        val baseFace = faceSize ?: minOf(width ?: size, height ?: size)
        val glyphSize =
            PadGlyphRules
                .glyphSizeDp(
                    defaultSizeDp = size.value,
                    faceSizeDp = baseFace.value,
                    enlarge = btn.enlargeIcon,
                    fullBleed = btn.fullBleedIcon,
                    isTableLayout = isTableLayout,
                ).dp

        val stretchModifier =
            if (btn.fullBleedIcon && width != null && height != null && width.value > 0f && height.value > 0f) {
                val minDim = minOf(width.value, height.value)
                if (minDim > 0f && (width.value != height.value)) {
                    Modifier.graphicsLayer {
                        scaleX = width.value / minDim
                        scaleY = height.value / minDim
                    }
                } else {
                    Modifier
                }
            } else {
                Modifier
            }

        MaterialSymbol(
            name = iconName,
            size = glyphSize,
            tint = tint,
            filled = btn.iconFilled,
            modifier = modifier.then(stretchModifier),
        )
    } else if (btn.showLabel) {
        fallback()
    }
}
