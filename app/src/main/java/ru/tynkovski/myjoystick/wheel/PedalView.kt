package ru.tynkovski.myjoystick.wheel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.conflate

@Composable
fun PedalControl(
    label: String,
    pressRampMs: Int,
    releaseRampMs: Int,
    fillColor: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    inverted: Boolean = false,
) {
    val progress = remember { Animatable(0f) }

    var touchValue by remember { mutableFloatStateOf(0f) }
    var pressed by remember { mutableStateOf(false) }

    val pressRamp by rememberUpdatedState(pressRampMs)
    val releaseRamp by rememberUpdatedState(releaseRampMs)
    val reportValue by rememberUpdatedState(onValueChange)

    LaunchedEffect(pressed) {
        if (pressed) {
            progress.animateTo(
                targetValue = touchValue,
                animationSpec = tween(durationMillis = pressRamp, easing = LinearEasing),
            )
            snapshotFlow { touchValue }
                .conflate()
                .collect { progress.snapTo(it) }
        } else {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = releaseRamp, easing = LinearEasing),
            )
        }
    }

    LaunchedEffect(progress) {
        snapshotFlow { progress.value }.collect { reportValue(it) }
    }

    val percent = remember { derivedStateOf { (progress.value * 100f).toInt() } }

    Box(
        modifier = modifier
            .drawBehind { drawPedalStrip(progress.value, fillColor, inverted) }
            .pointerInput(inverted) {
                fun pressureAt(y: Float): Float {
                    val fraction = (y / size.height.toFloat()).coerceIn(0f, 1f)
                    return if (inverted) fraction else 1f - fraction
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    touchValue = pressureAt(down.position.y)
                    pressed = true

                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        touchValue = pressureAt(change.position.y)
                    }

                    pressed = false
                }
            },
    ) {
        PedalReadout(
            label = label,
            percent = percent,
            inverted = inverted,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PedalReadout(
    label: String,
    percent: State<Int>,
    inverted: Boolean,
    modifier: Modifier = Modifier,
) {
    val alignment = if (inverted) Alignment.TopCenter else Alignment.BottomCenter
    val padding = if (inverted) {
        Modifier.padding(top = 18.dp)
    } else {
        Modifier.padding(bottom = 18.dp)
    }

    Box(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier
                .align(alignment)
                .then(if (inverted) Modifier.padding(top = 46.dp) else Modifier.padding(bottom = 46.dp)),
        )
        Text(
            text = "${percent.value}%",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier
                .align(alignment)
                .then(padding),
        )
    }
}

private fun DrawScope.drawPedalStrip(
    progressValue: Float,
    fillColor: Color,
    inverted: Boolean,
) {
    val inset = 5.dp.toPx()
    val stripSize = Size(size.width - inset * 2f, size.height - inset * 2f)
    if (stripSize.width <= 0f || stripSize.height <= 0f) return

    val topLeft = Offset(inset, inset)
    val corner = minOf(stripSize.width, stripSize.height) / 2f
    val cornerRadius = CornerRadius(corner, corner)

    drawRoundRect(
        color = Color(0xFF161616),
        topLeft = topLeft,
        size = stripSize,
        cornerRadius = cornerRadius,
    )
    drawRoundRect(
        color = Color(0xFF333333),
        topLeft = topLeft,
        size = stripSize,
        cornerRadius = cornerRadius,
        style = Stroke(width = 2.dp.toPx()),
    )

    val filled = stripSize.height * progressValue.coerceIn(0f, 1f)
    if (filled <= 0f) return

    val track = Path().apply {
        addRoundRect(RoundRect(rect = Rect(topLeft, stripSize), cornerRadius = cornerRadius))
    }
    clipPath(track) {
        drawRect(
            color = fillColor,
            topLeft = Offset(
                x = inset,
                y = if (inverted) inset else inset + stripSize.height - filled,
            ),
            size = Size(stripSize.width, filled),
        )
    }
}
