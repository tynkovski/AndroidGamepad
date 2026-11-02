package ru.tynkovski.myjoystick.wheel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.tynkovski.myjoystick.R

val EventButtonSize = 52.dp

private val ButtonIdle = Color(0x33FFFFFF)
private val ButtonDisabled = Color(0x14FFFFFF)

@Composable
fun EventButtons(
    enabled: Boolean,
    onEvent: (WheelEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    EventButton(
        symbol = stringResource(R.string.event_handbrake_symbol),
        enabled = enabled,
        onClick = { onEvent(WheelEvent.Handbrake) },
        modifier = modifier,
    )
}

@Composable
private fun EventButton(
    symbol: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.event_handbrake)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(EventButtonSize)
            .clip(CircleShape)
            .background(if (enabled) ButtonIdle else ButtonDisabled, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Text(
            text = symbol,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.3f),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun EventButtonsPreview() {
    Box {
        EventButtons(enabled = true, onEvent = {})
    }
}
