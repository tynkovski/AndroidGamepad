package ru.tynkovski.myjoystick.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.tynkovski.myjoystick.R
import ru.tynkovski.myjoystick.ui.theme.MyJoystickTheme
import ru.tynkovski.myjoystick.wheel.ControlMode
import ru.tynkovski.myjoystick.wheel.EventProtocol
import ru.tynkovski.myjoystick.wheel.WheelProtocol
import ru.tynkovski.myjoystick.wheel.WheelSettings
import ru.tynkovski.myjoystick.wheel.WheelState
import kotlin.math.roundToInt

private val ModeTabCorner = 12.dp

@Composable
fun SettingsScreen(
    initial: WheelSettings,
    onSave: (WheelSettings) -> Unit,
    onBack: () -> Unit,
    onConnect: (WheelSettings) -> Unit,
    onDisconnect: () -> Unit,
    tiltAvailable: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val status by WheelState.status.collectAsStateWithLifecycle()

    var ip by remember(initial) { mutableStateOf(initial.ip) }
    var port by remember(initial) { mutableStateOf(initial.port.toString()) }
    var eventPort by remember(initial) { mutableStateOf(initial.eventPort.toString()) }
    var controlMode by remember(initial) { mutableStateOf(initial.controlMode) }
    var lockToLock by remember(initial) { mutableStateOf(initial.lockToLockDegrees) }
    var pressRamp by remember(initial) { mutableStateOf(initial.pressRampMs.toString()) }
    var releaseRamp by remember(initial) { mutableStateOf(initial.releaseRampMs.toString()) }
    var brakeInverted by remember(initial) { mutableStateOf(initial.brakeAxisInverted) }
    var tiltSensitivity by remember(initial) { mutableFloatStateOf(initial.tiltSensitivity) }
    var axisWidth by remember(initial) { mutableFloatStateOf(initial.axisWidthFraction) }

    fun formSettings(): WheelSettings = WheelSettings(
        ip = ip,
        port = port.toIntOrNull()
            ?.takeIf { it in 1..65535 }
            ?: WheelProtocol.DEFAULT_PORT,
        eventPort = eventPort.toIntOrNull()
            ?.takeIf { it in 1..65535 }
            ?: EventProtocol.DEFAULT_EVENT_PORT,
        controlMode = controlMode,
        lockToLockDegrees = lockToLock,
        pressRampMs = pressRamp.toIntOrNull()
            ?.coerceIn(WheelSettings.MIN_RAMP_MS, WheelSettings.MAX_RAMP_MS)
            ?: WheelSettings.DEFAULT_PRESS_RAMP_MS,
        releaseRampMs = releaseRamp.toIntOrNull()
            ?.coerceIn(WheelSettings.MIN_RAMP_MS, WheelSettings.MAX_RAMP_MS)
            ?: WheelSettings.DEFAULT_RELEASE_RAMP_MS,
        brakeAxisInverted = brakeInverted,
        tiltSensitivity = WheelSettings.snapTiltSensitivity(tiltSensitivity),
        axisWidthFraction = WheelSettings.snapAxisWidth(axisWidth),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.settings_back))
            }
        }

        SectionTitle(stringResource(R.string.settings_section_connection))

        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it.trim() },
            label = { Text(stringResource(R.string.settings_ip_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = port,
                onValueChange = { value -> port = value.filter(Char::isDigit).take(5) },
                label = { Text(stringResource(R.string.settings_port_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = eventPort,
                onValueChange = { value -> eventPort = value.filter(Char::isDigit).take(5) },
                label = { Text(stringResource(R.string.settings_event_port_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onConnect(formSettings()) },
                enabled = !status.isRunning && ip.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.settings_connect)) }
            OutlinedButton(
                onClick = onDisconnect,
                enabled = status.isRunning,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.settings_disconnect)) }
        }

        StatusCard(
            isRunning = status.isRunning,
            target = status.target,
            packetsSent = status.packetsSent,
            error = status.error,
        )

        HorizontalDivider()

        SectionTitle(stringResource(R.string.settings_section_control))

        ModeTabs(
            controlMode = controlMode,
            tiltAvailable = tiltAvailable,
            onSelect = { mode -> controlMode = mode },
        )
        if (!tiltAvailable) {
            Text(
                text = stringResource(R.string.settings_control_mode_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        HorizontalDivider()

        when (controlMode) {
            ControlMode.WHEEL -> LockToLockSection(
                lockToLock = lockToLock,
                onLockToLockChange = { degrees -> lockToLock = degrees },
            )

            ControlMode.TILT -> TiltSection(
                sensitivity = tiltSensitivity,
                onSensitivityChange = { value -> tiltSensitivity = value },
            )
        }

        HorizontalDivider()

        SectionTitle(stringResource(R.string.settings_section_pedals))

        AxisWidthSection(
            axisWidth = axisWidth,
            onAxisWidthChange = { value -> axisWidth = value },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = pressRamp,
                onValueChange = { value -> pressRamp = value.filter(Char::isDigit).take(4) },
                label = { Text(stringResource(R.string.settings_press_ramp)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = releaseRamp,
                onValueChange = { value -> releaseRamp = value.filter(Char::isDigit).take(4) },
                label = { Text(stringResource(R.string.settings_release_ramp)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_brake_axis_inverted),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = brakeInverted,
                onCheckedChange = { brakeInverted = it },
            )
        }

        Button(
            onClick = { onSave(formSettings()) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.settings_save)) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ModeTabs(
    controlMode: ControlMode,
    tiltAvailable: Boolean,
    onSelect: (ControlMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ModeTabCorner))
            .background(Color(0xFF1C1C1C), RoundedCornerShape(ModeTabCorner))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        ModeTab(
            label = stringResource(R.string.settings_tab_wheel),
            selected = controlMode == ControlMode.WHEEL,
            enabled = true,
            onSelect = { onSelect(ControlMode.WHEEL) },
            modifier = Modifier.weight(1f),
        )
        ModeTab(
            label = stringResource(R.string.settings_tab_tilt),
            selected = controlMode == ControlMode.TILT,
            enabled = tiltAvailable,
            onSelect = { onSelect(ControlMode.TILT) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ModeTab(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = when {
        !enabled -> Color.White.copy(alpha = 0.35f)
        selected -> Color.White
        else -> Color.White.copy(alpha = 0.7f)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(ModeTabCorner))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                else Color.Transparent,
                RoundedCornerShape(ModeTabCorner),
            )
            .clickable(enabled = enabled && !selected, onClick = onSelect)
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TiltSection(
    sensitivity: Float,
    onSensitivityChange: (Float) -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_tilt_hint),
        style = MaterialTheme.typography.bodySmall,
        color = Color.Gray,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.settings_tilt_sensitivity),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(
                R.string.settings_tilt_sensitivity_value,
                (WheelSettings.snapTiltSensitivity(sensitivity) * 100).roundToInt(),
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    Slider(
        value = WheelSettings.snapTiltSensitivity(sensitivity),
        onValueChange = { value ->
            onSensitivityChange(WheelSettings.snapTiltSensitivity(value))
        },
        valueRange = WheelSettings.MIN_TILT_SENSITIVITY..WheelSettings.MAX_TILT_SENSITIVITY,
        steps = tiltSensitivitySteps(),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(
                R.string.settings_tilt_sensitivity_value,
                (WheelSettings.MIN_TILT_SENSITIVITY * 100).roundToInt(),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
        )
        Text(
            text = stringResource(
                R.string.settings_tilt_sensitivity_value,
                (WheelSettings.MAX_TILT_SENSITIVITY * 100).roundToInt(),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
        )
    }
}

private fun tiltSensitivitySteps(): Int {
    val range = WheelSettings.MAX_TILT_SENSITIVITY - WheelSettings.MIN_TILT_SENSITIVITY
    return (range / WheelSettings.TILT_SENSITIVITY_STEP).roundToInt() - 1
}

@Composable
private fun AxisWidthSection(
    axisWidth: Float,
    onAxisWidthChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.settings_axis_width),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(
                R.string.settings_axis_width_value,
                (WheelSettings.snapAxisWidth(axisWidth) * 100).roundToInt(),
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    Slider(
        value = WheelSettings.snapAxisWidth(axisWidth),
        onValueChange = { value ->
            onAxisWidthChange(WheelSettings.snapAxisWidth(value))
        },
        valueRange = WheelSettings.MIN_AXIS_WIDTH_FRACTION..WheelSettings.MAX_AXIS_WIDTH_FRACTION,
        steps = axisWidthSteps(),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(
                R.string.settings_axis_width_value,
                (WheelSettings.MIN_AXIS_WIDTH_FRACTION * 100).roundToInt(),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
        )
        Text(
            text = stringResource(
                R.string.settings_axis_width_value,
                (WheelSettings.MAX_AXIS_WIDTH_FRACTION * 100).roundToInt(),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
        )
    }
}

private fun axisWidthSteps(): Int {
    val range = WheelSettings.MAX_AXIS_WIDTH_FRACTION - WheelSettings.MIN_AXIS_WIDTH_FRACTION
    return (range / WheelSettings.AXIS_WIDTH_STEP).roundToInt() - 1
}

@Composable
private fun LockToLockSection(
    lockToLock: Int,
    onLockToLockChange: (Int) -> Unit,
) {
    SectionTitle(stringResource(R.string.settings_tab_wheel))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.settings_lock_to_lock),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.settings_lock_to_lock_degrees, lockToLock),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    val lockOptions = WheelSettings.LOCK_TO_LOCK_OPTIONS.asReversed()
    Slider(
        value = lockOptions.indexOf(lockToLock).coerceAtLeast(0).toFloat(),
        onValueChange = { index ->
            onLockToLockChange(lockOptions[index.roundToInt().coerceIn(lockOptions.indices)])
        },
        valueRange = 0f..(lockOptions.size - 1).toFloat(),
        steps = lockOptions.size - 2,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        lockOptions.forEach { option ->
            Text(
                text = stringResource(R.string.settings_lock_to_lock_degrees, option),
                style = MaterialTheme.typography.labelSmall,
                color = if (option == lockToLock) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Gray
                },
            )
        }
    }
}

@Composable
private fun StatusCard(
    isRunning: Boolean,
    target: String,
    packetsSent: Long,
    error: String?,
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1C))) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = if (isRunning) {
                    stringResource(R.string.status_streaming, target)
                } else {
                    stringResource(R.string.status_disconnected)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isRunning) MaterialTheme.colorScheme.primary else Color.Gray,
            )
            Text(
                text = "${stringResource(R.string.settings_packets)}: $packetsSent",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
            )
            error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun SettingsWheelPreview() {
    MyJoystickTheme(darkTheme = true, dynamicColor = false) {
        SettingsScreen(
            initial = WheelSettings(
                ip = "192.168.1.42",
                controlMode = ControlMode.WHEEL,
            ),
            onSave = {},
            onBack = {},
            onConnect = {},
            onDisconnect = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun SettingsTiltPreview() {
    MyJoystickTheme(darkTheme = true, dynamicColor = false) {
        SettingsScreen(
            initial = WheelSettings(
                ip = "192.168.1.42",
                controlMode = ControlMode.TILT,
            ),
            onSave = {},
            onBack = {},
            onConnect = {},
            onDisconnect = {},
        )
    }
}

@Preview(showBackground = true, name = "No tilt sensor", widthDp = 411, heightDp = 891)
@Composable
private fun SettingsNoSensorPreview() {
    MyJoystickTheme(darkTheme = true, dynamicColor = false) {
        SettingsScreen(
            initial = WheelSettings(controlMode = ControlMode.WHEEL),
            onSave = {},
            onBack = {},
            onConnect = {},
            onDisconnect = {},
            tiltAvailable = false,
        )
    }
}
