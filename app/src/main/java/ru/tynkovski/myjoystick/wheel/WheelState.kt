package ru.tynkovski.myjoystick.wheel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WheelStatus(
    val isRunning: Boolean = false,
    val target: String = "",
    val packetsSent: Long = 0L,
    val error: String? = null,
)

object WheelState {

    private val _status = MutableStateFlow(WheelStatus())
    val status: StateFlow<WheelStatus> = _status.asStateFlow()

    fun update(block: (WheelStatus) -> WheelStatus) {
        _status.value = block(_status.value)
    }
}
