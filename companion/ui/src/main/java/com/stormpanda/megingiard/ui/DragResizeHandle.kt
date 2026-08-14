package com.stormpanda.megingiard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset

private const val HANDLE_INDICATOR_ALPHA = 0.5f

@Composable
internal fun DragResizeHandle(
    offset: IntOffset,
    touchWidth: Dp,
    touchHeight: Dp,
    indicatorSize: Dp,
    indicatorCorner: Dp,
    color: Color,
    onDragStart: () -> Unit,
    onDrag: (Float, Float) -> Unit,
) {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    Box(
        modifier =
            Modifier
                .offset { offset }
                .size(width = touchWidth, height = touchHeight)
                .pointerInput(Unit) {
                    var accumulatedX = 0f
                    var accumulatedY = 0f
                    detectDragGestures(
                        onDragStart = {
                            accumulatedX = 0f
                            accumulatedY = 0f
                            currentOnDragStart()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedX += dragAmount.x
                            accumulatedY += dragAmount.y
                            currentOnDrag(accumulatedX, accumulatedY)
                        },
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(indicatorSize)
                    .background(color.copy(alpha = HANDLE_INDICATOR_ALPHA), RoundedCornerShape(indicatorCorner)),
        )
    }
}
