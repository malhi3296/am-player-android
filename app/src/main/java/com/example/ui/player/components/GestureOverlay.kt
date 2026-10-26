package com.example.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    gestureControlsEnabled: Boolean = true,
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

    // Double tap ripple state
    var showLeftRipple by remember { mutableStateOf(false) }
    var showRightRipple by remember { mutableStateOf(false) }
    val leftScale = remember { Animatable(0.6f) }
    val rightScale = remember { Animatable(0.6f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("gesture_overlay")
            .pointerInput(isLocked, gestureControlsEnabled) {
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

                if (!gestureControlsEnabled) {
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
                                val now = System.currentTimeMillis()
                                val tapOffset = pointers.firstOrNull()?.position ?: startPos
                                val diffX = abs(tapOffset.x - lastTapPos.x)
                                val diffY = abs(tapOffset.y - lastTapPos.y)

                                if (now - lastTapTime < 350L && diffX < 80f && diffY < 80f) {
                                    // Double tap recognized
                                    lastTapTime = 0L
                                    when {
                                        tapOffset.x < width * 0.35f -> {
                                            onDoubleTapLeft()
                                            scope.launch {
                                                showLeftRipple = true
                                                leftScale.snapTo(0.6f)
                                                leftScale.animateTo(1.2f, tween(350))
                                                delay(100)
                                                showLeftRipple = false
                                            }
                                        }
                                        tapOffset.x > width * 0.65f -> {
                                            onDoubleTapRight()
                                            scope.launch {
                                                showRightRipple = true
                                                rightScale.snapTo(0.6f)
                                                rightScale.animateTo(1.2f, tween(350))
                                                delay(100)
                                                showRightRipple = false
                                            }
                                        }
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
                                    currentDragMode = if (isLeftSide) DragMode.VERTICAL_LEFT else if (isRightSide) DragMode.VERTICAL_RIGHT else DragMode.NONE
                                } else {
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
    ) {
        // Double-Tap Left Ripple Animation (-10s)
        AnimatedVisibility(
            visible = showLeftRipple,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(250)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(0.35f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(topEnd = 160.dp, bottomEnd = 160.dp))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.scale(leftScale.value)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "-10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "-10 sec",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Double-Tap Right Ripple Animation (+10s)
        AnimatedVisibility(
            visible = showRightRipple,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(250)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.35f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(topStart = 160.dp, bottomStart = 160.dp))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.scale(rightScale.value)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "+10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "+10 sec",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
