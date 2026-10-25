package com.example.ui.player.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import kotlin.math.abs

private enum class DragMode {
    NONE,
    VERTICAL_LEFT,
    VERTICAL_RIGHT,
    HORIZONTAL_SEEK,
    PINCH_ZOOM
}

@Composable
fun GestureOverlay(
    isLocked: Boolean,
    onSingleTap: () -> Unit,
    onDoubleTapLeft: () -> Unit,
    onDoubleTapRight: () -> Unit,
    onDoubleTapCenter: () -> Unit,
    onVerticalSwipe: (isRightSide: Boolean, deltaFraction: Float) -> Unit,
    onHorizontalSeekDrag: (deltaFraction: Float) -> Unit,
    onHorizontalSeekEnd: () -> Unit,
    onPinchZoom: (scaleMultiplier: Float, panX: Float, panY: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapPos by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("gesture_overlay")
            .pointerInput(isLocked) {
                if (isLocked) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                        onSingleTap()
                    }
                    return@pointerInput
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()
                    val startPos = down.position
                    val isRightSide = startPos.x > width * 0.65f
                    val isLeftSide = startPos.x < width * 0.35f

                    var currentDragMode = DragMode.NONE
                    var accumulatedX = 0f
                    var accumulatedY = 0f
                    val touchSlop = 20f

                    while (true) {
                        val event: PointerEvent = awaitPointerEvent()
                        val pointers = event.changes

                        if (pointers.all { !it.pressed }) {
                            // Touch released / up
                            if (currentDragMode == DragMode.HORIZONTAL_SEEK) {
                                onHorizontalSeekEnd()
                            } else if (currentDragMode == DragMode.NONE) {
                                // Check single tap or double tap
                                val now = System.currentTimeMillis()
                                val tapOffset = pointers.firstOrNull()?.position ?: startPos
                                val diffX = abs(tapOffset.x - lastTapPos.x)
                                val diffY = abs(tapOffset.y - lastTapPos.y)

                                if (now - lastTapTime < 350L && diffX < 80f && diffY < 80f) {
                                    // Double tap recognized
                                    lastTapTime = 0L
                                    when {
                                        tapOffset.x < width * 0.35f -> onDoubleTapLeft()
                                        tapOffset.x > width * 0.65f -> onDoubleTapRight()
                                        else -> onDoubleTapCenter()
                                    }
                                } else {
                                    lastTapTime = now
                                    lastTapPos = tapOffset
                                    onSingleTap()
                                }
                            }
                            break
                        }

                        // Multitouch: Pinch to zoom
                        if (pointers.size >= 2) {
                            currentDragMode = DragMode.PINCH_ZOOM
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            if (zoom != 1f || pan != Offset.Zero) {
                                onPinchZoom(zoom, pan.x, pan.y)
                                pointers.forEach { it.consume() }
                            }
                            continue
                        }

                        // Single pointer drag handling
                        val pointer = pointers.first()
                        val change = pointer.positionChange()
                        accumulatedX += change.x
                        accumulatedY += change.y

                        if (currentDragMode == DragMode.NONE) {
                            if (abs(accumulatedX) > touchSlop || abs(accumulatedY) > touchSlop) {
                                if (abs(accumulatedY) > abs(accumulatedX)) {
                                    // Vertical drag (edge swipe)
                                    currentDragMode = if (isLeftSide) DragMode.VERTICAL_LEFT else if (isRightSide) DragMode.VERTICAL_RIGHT else DragMode.NONE
                                } else {
                                    // Horizontal drag (timeline scrub)
                                    currentDragMode = DragMode.HORIZONTAL_SEEK
                                }
                            }
                        }

                        when (currentDragMode) {
                            DragMode.VERTICAL_LEFT -> {
                                val deltaFraction = -change.y / height
                                onVerticalSwipe(false, deltaFraction)
                                pointer.consume()
                            }
                            DragMode.VERTICAL_RIGHT -> {
                                val deltaFraction = -change.y / height
                                onVerticalSwipe(true, deltaFraction)
                                pointer.consume()
                            }
                            DragMode.HORIZONTAL_SEEK -> {
                                val deltaFraction = accumulatedX / width
                                onHorizontalSeekDrag(deltaFraction)
                                pointer.consume()
                            }
                            else -> Unit
                        }
                    }
                }
            }
    )
}
