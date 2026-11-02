package ru.tynkovski.myjoystick.wheel

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.tynkovski.myjoystick.R
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.time.Duration.Companion.milliseconds

private val PadSize = 156.dp

private const val LabelFontRatio = 22f / 132f

private val PadCorner = 16.dp

private val SwipeThreshold = 34.dp

private const val FLASH_MS = 220L

private val LabelInset = 8.dp

private val PadIdle = Color(0x1FFFFFFF)
private val PadDisabled = Color(0x0DFFFFFF)

private val LabelIdle = Color(0x59FFFFFF)
private val LabelActive = Color.White
private val LabelDisabled = Color(0x26FFFFFF)

private val LabelReserved = Color(0x24FFFFFF)

private enum class GearSlot(val event: WheelEvent?) {
    ShiftUp(WheelEvent.ShiftUp),
    ShiftDown(WheelEvent.ShiftDown),
    Neutral(WheelEvent.Neutral),
    Reserved(null),
}

@Composable
fun GearPadControl(
    enabled: Boolean,
    onEvent: (WheelEvent) -> Unit,
    modifier: Modifier = Modifier,
    inverted: Boolean = false,
    size: Dp = PadSize,
) {
    val thresholdPx = with(LocalDensity.current) { SwipeThreshold.toPx() }
    val currentOnEvent by rememberUpdatedState(onEvent)
    val description = stringResource(
        if (inverted) R.string.gear_pad_description_inverted else R.string.gear_pad_description,
    )
    val labelFontSize = (size.value * LabelFontRatio).sp

    var flash by remember { mutableStateOf<GearSlot?>(null) }

    LaunchedEffect(flash) {
        if (flash != null) {
            delay(FLASH_MS.milliseconds)
            flash = null
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(PadCorner))
            .background(if (enabled) PadIdle else PadDisabled, RoundedCornerShape(PadCorner))
            .then(
                if (enabled) {
                    Modifier.pointerInput(thresholdPx, inverted) {
                        var dx = 0f
                        var dy = 0f
                        var fired = emptySet<GearSlot>()

                        detectDragGestures(
                            onDragStart = {
                                dx = 0f
                                dy = 0f
                                fired = emptySet()
                            },
                            onDragCancel = {
                                dx = 0f
                                dy = 0f
                                fired = emptySet()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dx += dragAmount.x
                                dy += dragAmount.y

                                while (hypot(dx, dy) >= thresholdPx) {
                                    val slot = slotOf(dx, dy, inverted)
                                    if (slot in fired) break

                                    val scale = thresholdPx / hypot(dx, dy)
                                    dx -= dx * scale
                                    dy -= dy * scale

                                    fired = fired + slot
                                    slot.event?.let { event ->
                                        flash = slot
                                        currentOnEvent(event)
                                    }
                                }
                            },
                            onDragEnd = {
                                dx = 0f
                                dy = 0f
                                fired = emptySet()
                            },
                        )
                    }
                } else {
                    Modifier
                },
            )
            .semantics { contentDescription = description },
    ) {
        GearLabel(
            symbol = stringResource(R.string.gear_pad_up_symbol),
            active = flash == GearSlot.ShiftUp,
            enabled = enabled,
            fontSize = labelFontSize,
            modifier = Modifier
                .align(if (inverted) Alignment.BottomCenter else Alignment.TopCenter)
                .padding(LabelInset),
        )
        GearLabel(
            symbol = stringResource(R.string.gear_pad_down_symbol),
            active = flash == GearSlot.ShiftDown,
            enabled = enabled,
            fontSize = labelFontSize,
            modifier = Modifier
                .align(if (inverted) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(LabelInset),
        )
        GearLabel(
            symbol = stringResource(R.string.event_neutral_symbol),
            active = flash == GearSlot.Neutral,
            enabled = enabled,
            fontSize = labelFontSize,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(LabelInset),
        )
        GearLabel(
            symbol = stringResource(R.string.gear_pad_reserved_symbol),
            active = false,
            enabled = enabled,
            reserved = true,
            fontSize = labelFontSize,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(LabelInset),
        )
    }
}

@Composable
private fun GearLabel(
    symbol: String,
    active: Boolean,
    enabled: Boolean,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    reserved: Boolean = false,
) {
    Text(
        text = symbol,
        modifier = modifier,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        color = when {
            !enabled -> LabelDisabled
            reserved -> LabelReserved
            active -> LabelActive
            else -> LabelIdle
        },
    )
}

private fun slotOf(dx: Float, dy: Float, inverted: Boolean): GearSlot = when {
    abs(dx) > abs(dy) -> if (dx < 0f) GearSlot.Neutral else GearSlot.Reserved
    dy < 0f -> if (inverted) GearSlot.ShiftDown else GearSlot.ShiftUp
    else -> if (inverted) GearSlot.ShiftUp else GearSlot.ShiftDown
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun GearPadControlPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GearPadControl(enabled = true, onEvent = {})
        GearPadControl(enabled = false, onEvent = {})
        GearPadControl(enabled = true, onEvent = {}, inverted = true)
    }
}
