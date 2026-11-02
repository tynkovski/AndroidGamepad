package ru.tynkovski.myjoystick.wheel

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.roundToInt

data class WheelSettings(
    val ip: String = "",
    val port: Int = WheelProtocol.DEFAULT_PORT,
    val eventPort: Int = EventProtocol.DEFAULT_EVENT_PORT,
    val controlMode: ControlMode = ControlMode.WHEEL,
    val lockToLockDegrees: Int = DEFAULT_LOCK_TO_LOCK,
    val pressRampMs: Int = DEFAULT_PRESS_RAMP_MS,
    val releaseRampMs: Int = DEFAULT_RELEASE_RAMP_MS,
    val brakeAxisInverted: Boolean = false,
    val tiltSensitivity: Float = DEFAULT_TILT_SENSITIVITY,
    val axisWidthFraction: Float = DEFAULT_AXIS_WIDTH_FRACTION,
) {
    val target: String get() = "$ip:$port"

    companion object {
        const val DEFAULT_LOCK_TO_LOCK = 960
        const val DEFAULT_PRESS_RAMP_MS = 100
        const val DEFAULT_RELEASE_RAMP_MS = 300

        val LOCK_TO_LOCK_OPTIONS = listOf(960, 720, 360, 305, 270)

        fun snapLockToLock(value: Int): Int =
            LOCK_TO_LOCK_OPTIONS.minByOrNull { abs(it - value) } ?: DEFAULT_LOCK_TO_LOCK

        const val MIN_RAMP_MS = 0
        const val MAX_RAMP_MS = 2000

        const val DEFAULT_TILT_SENSITIVITY = 1f
        const val MIN_TILT_SENSITIVITY = 0.5f
        const val MAX_TILT_SENSITIVITY = 1f
        const val TILT_SENSITIVITY_STEP = 0.05f

        fun snapTiltSensitivity(value: Float): Float {
            val steps = ((value.coerceIn(MIN_TILT_SENSITIVITY, MAX_TILT_SENSITIVITY) -
                MIN_TILT_SENSITIVITY) / TILT_SENSITIVITY_STEP).roundToInt()
            return (MIN_TILT_SENSITIVITY + steps * TILT_SENSITIVITY_STEP)
                .coerceIn(MIN_TILT_SENSITIVITY, MAX_TILT_SENSITIVITY)
        }

        const val DEFAULT_AXIS_WIDTH_FRACTION = 0.09f
        const val MIN_AXIS_WIDTH_FRACTION = 0.05f
        const val MAX_AXIS_WIDTH_FRACTION = 0.18f
        const val AXIS_WIDTH_STEP = 0.005f

        fun snapAxisWidth(value: Float): Float {
            val steps = ((value.coerceIn(MIN_AXIS_WIDTH_FRACTION, MAX_AXIS_WIDTH_FRACTION) -
                MIN_AXIS_WIDTH_FRACTION) / AXIS_WIDTH_STEP).roundToInt()
            return (MIN_AXIS_WIDTH_FRACTION + steps * AXIS_WIDTH_STEP)
                .coerceIn(MIN_AXIS_WIDTH_FRACTION, MAX_AXIS_WIDTH_FRACTION)
        }
    }
}

object WheelConfig {

    private lateinit var prefs: SharedPreferences

    private val _settings = MutableStateFlow(WheelSettings())
    val settings: StateFlow<WheelSettings> = _settings.asStateFlow()

    val current: WheelSettings get() = _settings.value

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _settings.value = read()
    }

    fun save(settings: WheelSettings) {
        prefs.edit {
            putString(KEY_IP, settings.ip)
            putInt(KEY_PORT, settings.port)
            putInt(KEY_EVENT_PORT, settings.eventPort)
            putString(KEY_CONTROL_MODE, settings.controlMode.name)
            putInt(KEY_LOCK_TO_LOCK, settings.lockToLockDegrees)
            putInt(KEY_PRESS_RAMP, settings.pressRampMs)
            putInt(KEY_RELEASE_RAMP, settings.releaseRampMs)
            putBoolean(KEY_BRAKE_INVERTED, settings.brakeAxisInverted)
            putFloat(KEY_TILT_SENSITIVITY, settings.tiltSensitivity)
            putFloat(KEY_AXIS_WIDTH, settings.axisWidthFraction)
        }
        _settings.value = settings
    }

    private fun read(): WheelSettings = WheelSettings(
        ip = prefs.getString(KEY_IP, "").orEmpty(),
        port = prefs.getInt(KEY_PORT, WheelProtocol.DEFAULT_PORT)
            .takeIf { it in 1..65535 } ?: WheelProtocol.DEFAULT_PORT,
        eventPort = prefs.getInt(KEY_EVENT_PORT, EventProtocol.DEFAULT_EVENT_PORT)
            .takeIf { it in 1..65535 } ?: EventProtocol.DEFAULT_EVENT_PORT,
        controlMode = prefs.getString(KEY_CONTROL_MODE, null)
            ?.let { name -> ControlMode.entries.firstOrNull { it.name == name } }
            ?: ControlMode.WHEEL,
        lockToLockDegrees = WheelSettings.snapLockToLock(
            prefs.getInt(KEY_LOCK_TO_LOCK, WheelSettings.DEFAULT_LOCK_TO_LOCK),
        ),
        pressRampMs = prefs.getInt(KEY_PRESS_RAMP, WheelSettings.DEFAULT_PRESS_RAMP_MS)
            .coerceIn(WheelSettings.MIN_RAMP_MS, WheelSettings.MAX_RAMP_MS),
        releaseRampMs = prefs.getInt(KEY_RELEASE_RAMP, WheelSettings.DEFAULT_RELEASE_RAMP_MS)
            .coerceIn(WheelSettings.MIN_RAMP_MS, WheelSettings.MAX_RAMP_MS),
        brakeAxisInverted = prefs.getBoolean(KEY_BRAKE_INVERTED, false),
        tiltSensitivity = prefs.getFloat(
            KEY_TILT_SENSITIVITY,
            WheelSettings.DEFAULT_TILT_SENSITIVITY,
        ).coerceIn(
            WheelSettings.MIN_TILT_SENSITIVITY,
            WheelSettings.MAX_TILT_SENSITIVITY,
        ),
        axisWidthFraction = prefs.getFloat(
            KEY_AXIS_WIDTH,
            WheelSettings.DEFAULT_AXIS_WIDTH_FRACTION,
        ).coerceIn(
            WheelSettings.MIN_AXIS_WIDTH_FRACTION,
            WheelSettings.MAX_AXIS_WIDTH_FRACTION,
        ),
    )

    private const val PREFS_NAME = "wheel"
    private const val KEY_IP = "ip"
    private const val KEY_PORT = "port"
    private const val KEY_EVENT_PORT = "evtPort"
    private const val KEY_CONTROL_MODE = "control_mode"
    private const val KEY_LOCK_TO_LOCK = "lock_to_lock_degrees"
    private const val KEY_PRESS_RAMP = "press_ramp_ms"
    private const val KEY_RELEASE_RAMP = "release_ramp_ms"
    private const val KEY_BRAKE_INVERTED = "brake_axis_inverted"
    private const val KEY_TILT_SENSITIVITY = "tilt_sensitivity"
    private const val KEY_AXIS_WIDTH = "axis_width_fraction"
}
