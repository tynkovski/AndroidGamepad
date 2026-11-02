package ru.tynkovski.myjoystick.wheel

object EventProtocol {

    const val DEFAULT_EVENT_PORT = 5556

    const val PACKET_SIZE = 1
}

enum class WheelEvent(private val code: Char) {
    ShiftUp('u'),

    ShiftDown('d'),

    Neutral('n'),

    Handbrake('h');

    val payload: ByteArray = byteArrayOf(code.code.toByte())
}
