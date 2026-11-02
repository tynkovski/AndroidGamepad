package ru.tynkovski.myjoystick.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.tynkovski.myjoystick.R
import ru.tynkovski.myjoystick.wheel.EventProtocol
import ru.tynkovski.myjoystick.wheel.WheelProtocol
import ru.tynkovski.myjoystick.wheel.WheelSettings
import ru.tynkovski.myjoystick.wheel.WheelState
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    initial: WheelSettings,
    onSave: (WheelSettings) -> Unit,
    onBack: () -> Unit,
    onConnect: (WheelSettings) -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status by WheelState.status.collectAsStateWithLifecycle()

    var ip by remember(initial) { mutableStateOf(initial.ip) }
    var port by remember(initial) { mutableStateOf(initial.port.toString()) }
    var eventPort by remember(initial) { mutableStateOf(initial.eventPort.toString()) }
    var lockToLock by remember(initial) { mutableStateOf(initial.lockToLockDegrees) }
    var pressRamp by remember(initial) { mutableStateOf(initial.pressRampMs.toString()) }
    var releaseRamp by remember(initial) { mutableStateOf(initial.releaseRampMs.toString()) }
    var brakeInverted by remember(initial) { mutableStateOf(initial.brakeAxisInverted) }

    fun formSettings(): WheelSettings = WheelSettings(
        ip = ip,
        port = port.toIntOrNull()
            ?.takeIf { it in 1..65535 }
            ?: WheelProtocol.DEFAULT_PORT,
        eventPort = eventPort.toIntOrNull()
            ?.takeIf { it in 1..65535 }
            ?: EventProtocol.DEFAULT_EVENT_PORT,
        lockToLockDegrees = lockToLock,
        pressRampMs = pressRamp.toIntOrNull()
            ?.coerceIn(WheelSettings.MIN_RAMP_MS, WheelSettings.MAX_RAMP_MS)
            ?: WheelSettings.DEFAULT_PRESS_RAMP_MS,
        releaseRampMs = releaseRamp.toIntOrNull()
            ?.coerceIn(WheelSettings.MIN_RAMP_MS, WheelSettings.MAX_RAMP_MS)
            ?: WheelSettings.DEFAULT_RELEASE_RAMP_MS,
        brakeAxisInverted = brakeInverted,
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

        SectionTitle(stringResource(R.string.settings_section_wheel))

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
                lockToLock = lockOptions[index.roundToInt().coerceIn(lockOptions.indices)]
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

        HorizontalDivider()

        SectionTitle(stringResource(R.string.settings_section_pedals))

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
