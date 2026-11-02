package ru.tynkovski.myjoystick.wheel

import java.nio.ByteBuffer
import java.nio.ByteOrder

object WheelProtocol {

    val MAGIC = byteArrayOf(0x57, 0x48, 0x4C, 0x32)

    const val PACKET_SIZE = 24
    const val DEFAULT_PORT = 5555
    const val SEND_HZ = 100

    const val SEQ_WRAP = 0x7FFFFFFF

    fun encode(
        seq: Int,
        steering: Float,
        throttle: Float,
        brake: Float,
        clutch: Float = 0f,
    ): ByteArray = ByteArray(PACKET_SIZE)
        .also { encodeInto(it, seq, steering, throttle, brake, clutch) }

    fun encodeInto(
        target: ByteArray,
        seq: Int,
        steering: Float,
        throttle: Float,
        brake: Float,
        clutch: Float = 0f,
    ) {
        require(target.size >= PACKET_SIZE) { "buffer must hold at least $PACKET_SIZE bytes" }
        val buffer = ByteBuffer.wrap(target).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(MAGIC)
        buffer.putInt(seq)
        buffer.putFloat(steering.coerceIn(-1f, 1f))
        buffer.putFloat(throttle.coerceIn(0f, 1f))
        buffer.putFloat(brake.coerceIn(0f, 1f))
        buffer.putFloat(clutch.coerceIn(0f, 1f))
    }
}
