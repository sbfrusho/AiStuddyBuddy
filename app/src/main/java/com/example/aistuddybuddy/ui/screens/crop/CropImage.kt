package com.example.aistuddybuddy.ui.screens.crop

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private const val MIN_SIZE = 0.05f

private enum class DragTarget {
    None, Move, Left, Top, Right, Bottom, TopLeft, TopRight, BottomLeft, BottomRight
}

/**
 * Shows [bitmap] with a resizable selection box. [cropRect] is normalized: its edges
 * are fractions (0..1) of the image width and height.
 */
@Composable
fun CropImage(
    bitmap: Bitmap,
    cropRect: Rect,
    onCropRectChange: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val density = LocalDensity.current
    val touchSlop = with(density) { 28.dp.toPx() }
    val handleRadius = with(density) { 8.dp.toPx() }
    val accent = Color(0xFF4D9BFF)

    BoxWithConstraints(modifier = modifier) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()
        val scale = minOf(containerWidth / bitmap.width, containerHeight / bitmap.height)
        val imageArea = Rect(
            offset = Offset(
                (containerWidth - bitmap.width * scale) / 2f,
                (containerHeight - bitmap.height * scale) / 2f
            ),
            size = Size(bitmap.width * scale, bitmap.height * scale)
        )

        val currentImageArea by rememberUpdatedState(imageArea)
        val currentCropRect by rememberUpdatedState(cropRect)
        val currentOnChange by rememberUpdatedState(onCropRectChange)
        var dragTarget by remember { mutableStateOf(DragTarget.None) }

        Image(
            bitmap = imageBitmap,
            contentDescription = "Scanned page",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { position ->
                            dragTarget = hitTest(
                                position,
                                currentCropRect.toScreen(currentImageArea),
                                touchSlop
                            )
                        },
                        onDragEnd = { dragTarget = DragTarget.None },
                        onDragCancel = { dragTarget = DragTarget.None },
                        onDrag = { change, dragAmount ->
                            if (dragTarget == DragTarget.None) return@detectDragGestures
                            change.consume()
                            val area = currentImageArea
                            currentOnChange(
                                currentCropRect.applyDrag(
                                    dragTarget,
                                    dx = dragAmount.x / area.width,
                                    dy = dragAmount.y / area.height
                                )
                            )
                        }
                    )
                }
        ) {
            val box = cropRect.toScreen(imageArea)
            val dim = Color.Black.copy(alpha = 0.55f)

            // Darken everything outside the selection.
            drawRect(dim, topLeft = Offset.Zero, size = Size(size.width, box.top))
            drawRect(dim, topLeft = Offset(0f, box.bottom), size = Size(size.width, size.height - box.bottom))
            drawRect(dim, topLeft = Offset(0f, box.top), size = Size(box.left, box.height))
            drawRect(dim, topLeft = Offset(box.right, box.top), size = Size(size.width - box.right, box.height))

            // Rule-of-thirds guides.
            val guide = Color.White.copy(alpha = 0.35f)
            for (i in 1..2) {
                val x = box.left + box.width * i / 3f
                val y = box.top + box.height * i / 3f
                drawLine(guide, Offset(x, box.top), Offset(x, box.bottom), strokeWidth = 1.dp.toPx())
                drawLine(guide, Offset(box.left, y), Offset(box.right, y), strokeWidth = 1.dp.toPx())
            }

            drawRect(accent, topLeft = box.topLeft, size = box.size, style = Stroke(width = 2.dp.toPx()))

            listOf(
                box.topLeft, box.topRight, box.bottomLeft, box.bottomRight,
                box.centerLeft, box.centerRight, box.topCenter, box.bottomCenter
            ).forEach { point ->
                drawCircle(Color.White, radius = handleRadius, center = point)
                drawCircle(accent, radius = handleRadius, center = point, style = Stroke(width = 2.dp.toPx()))
            }
        }
    }
}

private fun Rect.toScreen(imageArea: Rect) = Rect(
    left = imageArea.left + left * imageArea.width,
    top = imageArea.top + top * imageArea.height,
    right = imageArea.left + right * imageArea.width,
    bottom = imageArea.top + bottom * imageArea.height
)

private fun hitTest(position: Offset, box: Rect, slop: Float): DragTarget {
    val nearLeft = abs(position.x - box.left) < slop
    val nearRight = abs(position.x - box.right) < slop
    val nearTop = abs(position.y - box.top) < slop
    val nearBottom = abs(position.y - box.bottom) < slop
    val withinX = position.x in (box.left - slop)..(box.right + slop)
    val withinY = position.y in (box.top - slop)..(box.bottom + slop)

    return when {
        nearLeft && nearTop -> DragTarget.TopLeft
        nearRight && nearTop -> DragTarget.TopRight
        nearLeft && nearBottom -> DragTarget.BottomLeft
        nearRight && nearBottom -> DragTarget.BottomRight
        nearLeft && withinY -> DragTarget.Left
        nearRight && withinY -> DragTarget.Right
        nearTop && withinX -> DragTarget.Top
        nearBottom && withinX -> DragTarget.Bottom
        box.contains(position) -> DragTarget.Move
        else -> DragTarget.None
    }
}

private fun Rect.applyDrag(target: DragTarget, dx: Float, dy: Float): Rect {
    if (target == DragTarget.Move) {
        val moveX = dx.coerceIn(-left, 1f - right)
        val moveY = dy.coerceIn(-top, 1f - bottom)
        return translate(moveX, moveY)
    }

    var newLeft = left
    var newTop = top
    var newRight = right
    var newBottom = bottom

    if (target in setOf(DragTarget.Left, DragTarget.TopLeft, DragTarget.BottomLeft)) {
        newLeft = (left + dx).coerceIn(0f, right - MIN_SIZE)
    }
    if (target in setOf(DragTarget.Right, DragTarget.TopRight, DragTarget.BottomRight)) {
        newRight = (right + dx).coerceIn(left + MIN_SIZE, 1f)
    }
    if (target in setOf(DragTarget.Top, DragTarget.TopLeft, DragTarget.TopRight)) {
        newTop = (top + dy).coerceIn(0f, bottom - MIN_SIZE)
    }
    if (target in setOf(DragTarget.Bottom, DragTarget.BottomLeft, DragTarget.BottomRight)) {
        newBottom = (bottom + dy).coerceIn(top + MIN_SIZE, 1f)
    }
    return Rect(newLeft, newTop, newRight, newBottom)
}
