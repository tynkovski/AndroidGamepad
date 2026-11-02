package ru.tynkovski.myjoystick.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.tynkovski.myjoystick.R
import ru.tynkovski.myjoystick.ui.theme.MyJoystickTheme
import ru.tynkovski.myjoystick.wheel.ControlMode
import ru.tynkovski.myjoystick.wheel.EventButtons
import ru.tynkovski.myjoystick.wheel.GearPadControl
import ru.tynkovski.myjoystick.wheel.PedalControl
import ru.tynkovski.myjoystick.wheel.SteeringIndicator
import ru.tynkovski.myjoystick.wheel.WheelControl
import ru.tynkovski.myjoystick.wheel.WheelEvent
import ru.tynkovski.myjoystick.wheel.WheelSettings

private val GasColor = Color(0xFF2ECC71)
private val BrakeColor = Color(0xFFE74C3C)

private const val PEDAL_TILT_DEGREES = 0f

private const val STRIP_WIDTH_FRACTION = 0.09f

private val PedalGap = 8.dp

private val WheelInset = 8.dp

private val ScreenEdge = 16.dp

private val ButtonsPedalGap = 12.dp

private val GearPadGap = 38.dp

private val TiltHintInset = 18.dp

private const val TiltIndicatorWidthFraction = 1f

private val TiltIndicatorBottomInset = 2.dp

@Composable
fun ControlScreen(
    state: ControlInputState,
    lockToLockDegrees: Int,
    pressRampMs: Int,
    releaseRampMs: Int,
    brakeAxisInverted: Boolean,
    eventsEnabled: Boolean,
    onEvent: (WheelEvent) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    controlMode: ControlMode = ControlMode.WHEEL,
    axisWidthFraction: Float = STRIP_WIDTH_FRACTION,
    onRecenterTilt: () -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .displayCutoutPadding(),
    ) {
        val stripWidth = maxWidth * axisWidthFraction
        val stripHeight = maxHeight - (ScreenEdge * 2)

        when (controlMode) {
            ControlMode.TILT -> TiltLayout(
                state = state,
                lockToLockDegrees = lockToLockDegrees,
                stripWidth = stripWidth,
                stripHeight = stripHeight,
                pressRampMs = pressRampMs,
                releaseRampMs = releaseRampMs,
                brakeAxisInverted = brakeAxisInverted,
                eventsEnabled = eventsEnabled,
                onEvent = onEvent,
                onRecenterTilt = onRecenterTilt,
                onOpenSettings = onOpenSettings
            )

            ControlMode.WHEEL -> WheelLayout(
                state = state,
                lockToLockDegrees = lockToLockDegrees,
                stripWidth = stripWidth,
                stripHeight = stripHeight,
                pressRampMs = pressRampMs,
                releaseRampMs = releaseRampMs,
                brakeAxisInverted = brakeAxisInverted,
                eventsEnabled = eventsEnabled,
                onEvent = onEvent,
                onOpenSettings = onOpenSettings
            )
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.WheelLayout(
    state: ControlInputState,
    lockToLockDegrees: Int,
    stripWidth: Dp,
    stripHeight: Dp,
    pressRampMs: Int,
    releaseRampMs: Int,
    brakeAxisInverted: Boolean,
    eventsEnabled: Boolean,
    onEvent: (WheelEvent) -> Unit,
    onOpenSettings: () -> Unit,
) {
    WheelControl(
        angleDegrees = state.wheelAngleDegrees,
        onRotate = { delta -> state.rotateWheel(delta, lockToLockDegrees) },
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.5f)
            .padding(WheelInset),
    )

    Row(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(ScreenEdge),
        horizontalArrangement = Arrangement.spacedBy(GearPadGap),
    ) {
        Box(modifier = Modifier.fillMaxHeight()) {
            GearPadControl(
                enabled = eventsEnabled,
                onEvent = onEvent,
                inverted = brakeAxisInverted,
                modifier = Modifier.align(Alignment.Center),
            )
            EventButtons(
                enabled = eventsEnabled,
                onEvent = onEvent,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        PedalPair(
            state = state,
            stripWidth = stripWidth,
            stripHeight = stripHeight,
            pressRampMs = pressRampMs,
            releaseRampMs = releaseRampMs,
            brakeAxisInverted = brakeAxisInverted,
        )
    }

    SettingsButton(
        onClick = onOpenSettings,
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(12.dp),
    )
}

@Composable
private fun BoxWithConstraintsScope.TiltLayout(
    state: ControlInputState,
    lockToLockDegrees: Int,
    stripWidth: Dp,
    stripHeight: Dp,
    pressRampMs: Int,
    releaseRampMs: Int,
    brakeAxisInverted: Boolean,
    eventsEnabled: Boolean,
    onEvent: (WheelEvent) -> Unit,
    onRecenterTilt: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val steeringHalfTravel = (lockToLockDegrees.coerceAtLeast(2) / 2).toFloat()

    Row(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .padding(start = ScreenEdge),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GearPadGap),
    ) {
        PedalControl(
            label = stringResource(R.string.pedal_brake),
            pressRampMs = pressRampMs,
            releaseRampMs = releaseRampMs,
            fillColor = BrakeColor,
            onValueChange = state::updateBrake,
            inverted = brakeAxisInverted,
            modifier = Modifier
                .width(stripWidth)
                .height(stripHeight),
        )
        GearPadControl(
            enabled = eventsEnabled,
            onEvent = onEvent,
        )
    }

    Row(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(end = ScreenEdge),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GearPadGap),
    ) {
        GearPadControl(
            enabled = eventsEnabled,
            onEvent = onEvent,
            inverted = brakeAxisInverted,
        )
        PedalControl(
            label = stringResource(R.string.pedal_gas),
            pressRampMs = pressRampMs,
            releaseRampMs = releaseRampMs,
            fillColor = GasColor,
            onValueChange = state::updateThrottle,
            modifier = Modifier
                .width(stripWidth)
                .height(stripHeight),
        )
    }

    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .fillMaxHeight()
            .fillMaxWidth(0.5f)
            .padding(TiltHintInset),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ButtonsPedalGap),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ButtonsPedalGap),
            ) {
                SettingsButton(onClick = onOpenSettings)

                IconButton(
                    onClick = onRecenterTilt,
                    modifier = Modifier.background(Color(0x33FFFFFF), CircleShape),
                ) {
                    Text(
                        text = stringResource(R.string.control_mode_tilt_recenter_symbol),
                        fontSize = 24.sp,
                        color = Color.White,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            EventButtons(enabled = eventsEnabled, onEvent = onEvent)
        }
    }

    SteeringIndicator(
        steering = state.wheelAngleDegrees / steeringHalfTravel,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(TiltIndicatorWidthFraction)
            .padding(bottom = TiltIndicatorBottomInset),
    )
}

@Composable
private fun PedalPair(
    state: ControlInputState,
    stripWidth: Dp,
    stripHeight: Dp,
    pressRampMs: Int,
    releaseRampMs: Int,
    brakeAxisInverted: Boolean,
) {
    Row(
        modifier = Modifier.rotate(PEDAL_TILT_DEGREES),
        horizontalArrangement = Arrangement.spacedBy(PedalGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PedalControl(
            label = stringResource(R.string.pedal_brake),
            pressRampMs = pressRampMs,
            releaseRampMs = releaseRampMs,
            fillColor = BrakeColor,
            onValueChange = state::updateBrake,
            inverted = brakeAxisInverted,
            modifier = Modifier
                .width(stripWidth)
                .height(stripHeight),
        )
        PedalControl(
            label = stringResource(R.string.pedal_gas),
            pressRampMs = pressRampMs,
            releaseRampMs = releaseRampMs,
            fillColor = GasColor,
            onValueChange = state::updateThrottle,
            modifier = Modifier
                .width(stripWidth)
                .height(stripHeight),
        )
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier.background(Color(0x33FFFFFF), CircleShape),
    ) {
        Text(text = "⚙", fontSize = 24.sp, color = Color.White)
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape",
)
@Composable
private fun ControlScreenPreview() {
    MyJoystickTheme(darkTheme = true, dynamicColor = false) {
        ControlScreen(
            state = remember { ControlInputState() },
            lockToLockDegrees = WheelSettings.DEFAULT_LOCK_TO_LOCK,
            pressRampMs = WheelSettings.DEFAULT_PRESS_RAMP_MS,
            releaseRampMs = WheelSettings.DEFAULT_RELEASE_RAMP_MS,
            brakeAxisInverted = false,
            eventsEnabled = true,
            onEvent = {},
            onOpenSettings = {},
        )
    }
}

@Preview(
    showBackground = true,
    name = "Tilt mode",
    device = "spec:width=891dp,height=411dp,orientation=landscape",
)
@Composable
private fun ControlScreenTiltPreview() {
    MyJoystickTheme(darkTheme = true, dynamicColor = false) {
        ControlScreen(
            state = remember { ControlInputState() },
            lockToLockDegrees = WheelSettings.DEFAULT_LOCK_TO_LOCK,
            pressRampMs = WheelSettings.DEFAULT_PRESS_RAMP_MS,
            releaseRampMs = WheelSettings.DEFAULT_RELEASE_RAMP_MS,
            brakeAxisInverted = false,
            eventsEnabled = true,
            onEvent = {},
            onOpenSettings = {},
            controlMode = ControlMode.TILT,
        )
    }
}

@Preview(
    showBackground = true,
    name = "Disconnected",
    device = "spec:width=891dp,height=411dp,orientation=landscape",
)
@Composable
private fun ControlScreenDisconnectedPreview() {
    MyJoystickTheme(darkTheme = true, dynamicColor = false) {
        ControlScreen(
            state = remember { ControlInputState() },
            lockToLockDegrees = WheelSettings.DEFAULT_LOCK_TO_LOCK,
            pressRampMs = WheelSettings.DEFAULT_PRESS_RAMP_MS,
            releaseRampMs = WheelSettings.DEFAULT_RELEASE_RAMP_MS,
            brakeAxisInverted = false,
            eventsEnabled = false,
            onEvent = {},
            onOpenSettings = {},
        )
    }
}
