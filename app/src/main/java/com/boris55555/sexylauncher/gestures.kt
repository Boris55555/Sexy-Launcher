package com.boris55555.sexylauncher

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

fun Modifier.homeScreenGestures(
    gesturesEnabled: Boolean,
    favoritesArea: Rect?,
    onSwipeUp: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onFavoritesSwipeUp: () -> Unit,
    onFavoritesSwipeDown: () -> Unit
): Modifier = this.pointerInput(
    gesturesEnabled,
    favoritesArea,
    onSwipeUp,
    onSwipeLeft,
    onSwipeRight,
    onFavoritesSwipeUp,
    onFavoritesSwipeDown
) {
    if (gesturesEnabled) {
        var totalDrag = Offset.Zero
        var dragStartedInFavoritesArea = false

        detectDragGestures(
            onDragStart = {
                totalDrag = Offset.Zero
                dragStartedInFavoritesArea = favoritesArea?.contains(it) ?: false
            },
            onDrag = { change, dragAmount ->
                change.consume()
                totalDrag += dragAmount
            },
            onDragEnd = {
                val (x, y) = totalDrag
                val isHorizontal = abs(x) > abs(y)
                val isVertical = abs(y) > abs(x)

                if (dragStartedInFavoritesArea) {
                    if (isVertical) {
                        if (y < -20) {
                            onFavoritesSwipeUp()
                        } else if (y > 20) {
                            onFavoritesSwipeDown()
                        }
                    }
                } else { // Not in favorites
                    if (isVertical) {
                        if (y < -50) {
                            onSwipeUp()
                        }
                    } else if (isHorizontal) {
                        if (x > 50) {
                            onSwipeRight()
                        } else if (x < -50) {
                            onSwipeLeft()
                        }
                    }
                }
            }
        )
    }
}
