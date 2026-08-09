package com.tupaz.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

class SplitRectClipper(private val fraction: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val splitX = size.width * fraction.coerceIn(0f, 1f)
        return Outline.Rectangle(
            Rect(0f, 0f, splitX, size.height)
        )
    }
}

@Composable
fun SplitComparisonView(
    dividerFraction: Float,
    originalContent: @Composable () -> Unit,
    enhancedContent: @Composable () -> Unit,
    originalLabel: String = "Original (1080p)",
    enhancedLabel: String = "AI Enhanced (4K)",
    onDividerFractionChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var widthPx by remember { mutableFloatStateOf(1000f) }
    var heightPx by remember { mutableFloatStateOf(1000f) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val currentFraction by rememberUpdatedState(dividerFraction)

    val handleSizePx = with(density) { 40.dp.toPx() }
    val dividerWidthPx = with(density) { 2.dp.toPx() }
    val splitX = widthPx * currentFraction.coerceIn(0f, 1f)

    // Keep offsets within boundary as scale or view dimensions change
    LaunchedEffect(scale, widthPx, heightPx) {
        if (scale <= 1f) {
            offsetX = 0f
            offsetY = 0f
        } else {
            val limitX = (widthPx * (scale - 1f) / 2f).coerceAtLeast(0f)
            val limitY = (heightPx * (scale - 1f) / 2f).coerceAtLeast(0f)
            offsetX = offsetX.coerceIn(-limitX, limitX)
            offsetY = offsetY.coerceIn(-limitY, limitY)
        }
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .onSizeChanged {
                widthPx = it.width.toFloat().coerceAtLeast(1f)
                heightPx = it.height.toFloat().coerceAtLeast(1f)
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }
                    )
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val changes = event.changes
                            val pressedList = changes.filter { it.pressed }

                            if (pressedList.size == 1) {
                                // 1 Finger touch -> Always updates comparison slider fraction
                                val change = pressedList[0]
                                val dragDelta = change.position - change.previousPosition
                                if (dragDelta != Offset.Zero) {
                                    if (widthPx > 0) {
                                        val deltaFraction = dragDelta.x / widthPx
                                        onDividerFractionChange((currentFraction + deltaFraction).coerceIn(0f, 1f))
                                    }
                                    change.consume()
                                }
                            } else if (pressedList.size >= 2) {
                                // 2+ Finger touch -> Pinches to zoom & pans zoomed video
                                val p1 = pressedList[0].position
                                val p2 = pressedList[1].position
                                val prevP1 = pressedList[0].previousPosition
                                val prevP2 = pressedList[1].previousPosition

                                val currentDist = (p1 - p2).getDistance()
                                val prevDist = (prevP1 - prevP2).getDistance()

                                if (prevDist > 0f && currentDist > 0f) {
                                    val zoomFactor = currentDist / prevDist
                                    val newScale = (scale * zoomFactor).coerceIn(1f, 5f)
                                    scale = newScale

                                    val centroid = (p1 + p2) / 2f
                                    val prevCentroid = (prevP1 + prevP2) / 2f
                                    val panDelta = centroid - prevCentroid

                                    val limitX = (widthPx * (newScale - 1f) / 2f).coerceAtLeast(0f)
                                    val limitY = (heightPx * (newScale - 1f) / 2f).coerceAtLeast(0f)

                                    if (newScale > 1f) {
                                        offsetX = (offsetX + panDelta.x).coerceIn(-limitX, limitX)
                                        offsetY = (offsetY + panDelta.y).coerceIn(-limitY, limitY)
                                    } else {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }

                                pressedList.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            // Base Layer: AI Enhanced Video Content (Full Width, visible on the Right of splitX)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            ) {
                enhancedContent()
            }

            // Top Layer: Original Video Content (Clipped at screen coordinate splitX, visible on the Left of splitX)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(SplitRectClipper(currentFraction))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                ) {
                    originalContent()
                }
            }

            // Top Left Badge (Original) - Left side of the slider
            Box(
                modifier = Modifier
                    .padding(14.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(originalLabel, style = MaterialTheme.typography.labelSmall.copy(color = Color.White))
            }

            // Top Right Badge (AI Enhanced) - Right side of the slider
            Box(
                modifier = Modifier
                    .padding(14.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF8B5CF6).copy(alpha = 0.9f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .align(Alignment.TopEnd)
            ) {
                Text(enhancedLabel, style = MaterialTheme.typography.labelSmall.copy(color = Color.White))
            }

            // Vertical White Divider Line (2dp centered at splitX)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .align(Alignment.TopStart)
                    .offset { IntOffset((splitX - dividerWidthPx / 2f).roundToInt(), 0) }
                    .background(Color.White)
            )

            // Circular Drag Handle (40dp centered at splitX) with shadow & ↔ icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.CenterStart)
                    .offset { IntOffset((splitX - handleSizePx / 2f).roundToInt(), 0) }
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Drag comparison slider",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Zoom level indicator badge when zoomed in (> 1.05x)
            if (scale > 1.05f) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Text(
                        text = "${String.format("%.1f", scale)}x • Double tap to reset",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White)
                    )
                }
            }
        }
    }
}
