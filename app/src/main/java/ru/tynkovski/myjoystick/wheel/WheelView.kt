package ru.tynkovski.myjoystick.wheel

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

private const val RIM_FRACTION = 0.44f

private const val STROKE_FRACTION = 0.13f

private const val INNER_DEAD_ZONE_FRACTION = 0.3f

private fun rimRadius(width: Float, height: Float): Float = min(width, height) * RIM_FRACTION

@Composable
fun WheelControl(
    angleDegrees: Float,
    onRotate: (deltaDegrees: Float) -> Unit,
    modifier: Modifier = Modifier,
    rimColor: Color = Color(0xFF4A4A4A),
    accentColor: Color = Color(0xFFEDEDED),
) {
    Box(
        modifier = modifier
            .clipToBounds()
            .background(Color.Black)
            .drawBehind { drawWheel(angleDegrees, rimColor, accentColor) }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()

                    val hub = Offset(size.width / 2f, size.height / 2f)
                    val rim = rimRadius(size.width.toFloat(), size.height.toFloat())
                    val deadZone = rim * INNER_DEAD_ZONE_FRACTION

                    var lastAngle = angleOf(down.position, hub)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break

                        val distance = hypot(change.position.x - hub.x, change.position.y - hub.y)
                        if (distance < deadZone) {
                            lastAngle = angleOf(change.position, hub)
                            continue
                        }

                        val currentAngle = angleOf(change.position, hub)
                        val delta = shortestAngleDelta(lastAngle, currentAngle)
                        lastAngle = currentAngle

                        if (delta != 0f) {
                            onRotate(delta)
                            change.consume()
                        }
                    }
                }
            },
    )
}

private fun DrawScope.drawWheel(
    angleDegrees: Float,
    rimColor: Color,
    accentColor: Color,
) {
    val rim = rimRadius(size.width, size.height)
    if (rim <= 0f) return

    val hub = Offset(size.width / 2f, size.height / 2f)
    val stroke = rim * STROKE_FRACTION
    val topLeft = Offset(hub.x - rim, hub.y - rim)
    val arcSize = Size(rim * 2f, rim * 2f)

    drawCircle(color = rimColor, radius = rim, center = hub, style = Stroke(width = stroke))

    rotate(degrees = angleDegrees, pivot = hub) {
        listOf(90f, 210f, 330f).forEach { degrees ->
            val radians = Math.toRadians(degrees.toDouble())
            drawLine(
                color = rimColor,
                start = hub,
                end = Offset(
                    x = hub.x + cos(radians).toFloat() * rim * 0.9f,
                    y = hub.y + sin(radians).toFloat() * rim * 0.9f,
                ),
                strokeWidth = stroke * 0.7f,
            )
        }

        drawArc(
            color = accentColor,
            startAngle = -97f,
            sweepAngle = 14f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke * 1.25f),
        )

        drawCircle(color = accentColor, radius = rim * 0.15f, center = hub)
    }
}

private fun angleOf(point: Offset, hub: Offset): Float =
    Math.toDegrees(
        atan2((point.y - hub.y).toDouble(), (point.x - hub.x).toDouble())
    ).toFloat()

private fun shortestAngleDelta(from: Float, to: Float): Float {
    var delta = to - from
    while (delta > 180f) delta -= 360f
    while (delta < -180f) delta += 360f
    return delta
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 400, heightDp = 400)
@Composable
private fun WheelControlPreview() {
    WheelControl(angleDegrees = 0f, onRotate = {})
}
