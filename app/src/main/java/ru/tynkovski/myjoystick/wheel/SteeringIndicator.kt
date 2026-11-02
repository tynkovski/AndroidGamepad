package ru.tynkovski.myjoystick.wheel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.tynkovski.myjoystick.R
import kotlin.math.abs
import kotlin.math.roundToInt

private val Thickness = 4.dp

private val TrackColor = Color(0x14FFFFFF)

private val SteeringColor = Color(0xCCFFFFFF)

@Composable
fun SteeringIndicator(
    steering: Float,
    modifier: Modifier = Modifier,
) {
    val value = steering.coerceIn(-1f, 1f)
    val description = stringResource(R.string.steering_state, (value * 100f).roundToInt())

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(Thickness)
            .semantics { contentDescription = description },
    ) {
        drawRect(color = TrackColor, size = size)

        val centre = size.width / 2f
        val filled = centre * abs(value)
        if (filled <= 0f) return@Canvas

        drawRect(
            color = SteeringColor,
            topLeft = Offset(if (value < 0f) centre - filled else centre, 0f),
            size = Size(filled, size.height),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 400)
@Composable
private fun SteeringIndicatorPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SteeringIndicator(steering = 0f)
        SteeringIndicator(steering = 0.45f)
        SteeringIndicator(steering = -0.8f)
        SteeringIndicator(steering = 1f)
    }
}
