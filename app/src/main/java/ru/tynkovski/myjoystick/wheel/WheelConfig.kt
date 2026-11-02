package ru.tynkovski.myjoystick.wheel

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class WheelSettings(
    val ip: String = "",
    val port: Int = WheelProtocol.DEFAULT_PORT,
    val eventPort: Int = EventProtocol.DEFAULT_EVENT_PORT,
    val lockToLockDegrees: Int = DEFAULT_LOCK_TO_LOCK,
    val pressRampMs: Int = DEFAULT_PRESS_RAMP_MS,
    val releaseRampMs: Int = DEFAULT_RELEASE_RAMP_MS,
    val brakeAxisInverted: Boolean = false,
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
            putInt(KEY_LOCK_TO_LOCK, settings.lockToLockDegrees)
            putInt(KEY_PRESS_RAMP, settings.pressRampMs)
            putInt(KEY_RELEASE_RAMP, settings.releaseRampMs)
            putBoolean(KEY_BRAKE_INVERTED, settings.brakeAxisInverted)
        }
        _settings.value = settings
    }

    private fun read(): WheelSettings = WheelSettings(
        ip = prefs.getString(KEY_IP, "").orEmpty(),
        port = prefs.getInt(KEY_PORT, WheelProtocol.DEFAULT_PORT)
            .takeIf { it in 1..65535 } ?: WheelProtocol.DEFAULT_PORT,
        eventPort = prefs.getInt(KEY_EVENT_PORT, EventProtocol.DEFAULT_EVENT_PORT)
            .takeIf { it in 1..65535 } ?: EventProtocol.DEFAULT_EVENT_PORT,
        lockToLockDegrees = WheelSettings.snapLockToLock(
            prefs.getInt(KEY_LOCK_TO_LOCK, WheelSettings.DEFAULT_LOCK_TO_LOCK),
        ),
        pressRampMs = prefs.getInt(KEY_PRESS_RAMP, WheelSettings.DEFAULT_PRESS_RAMP_MS)
            .coerceIn(WheelSettings.MIN_RAMP_MS, WheelSettings.MAX_RAMP_MS),
        releaseRampMs = prefs.getInt(KEY_RELEASE_RAMP, WheelSettings.DEFAULT_RELEASE_RAMP_MS)
            .coerceIn(WheelSettings.MIN_RAMP_MS, WheelSettings.MAX_RAMP_MS),
        brakeAxisInverted = prefs.getBoolean(KEY_BRAKE_INVERTED, false),
    )

    private const val PREFS_NAME = "wheel"
    private const val KEY_IP = "ip"
    private const val KEY_PORT = "port"
    private const val KEY_EVENT_PORT = "evtPort"
    private const val KEY_LOCK_TO_LOCK = "lock_to_lock_degrees"
    private const val KEY_PRESS_RAMP = "press_ramp_ms"
    private const val KEY_RELEASE_RAMP = "release_ramp_ms"
    private const val KEY_BRAKE_INVERTED = "brake_axis_inverted"
}
