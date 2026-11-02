package ru.tynkovski.myjoystick.wheel

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WheelProtocolTest {

    @Test
    fun `packet is exactly 24 bytes`() {
        assertEquals(24, WheelProtocol.encode(0, 0f, 0f, 0f, 0f).size)
    }

    @Test
    fun `magic bytes are ASCII WHL2`() {
        val packet = WheelProtocol.encode(0, 0f, 0f, 0f, 0f)
        assertArrayEquals(
            byteArrayOf(0x57, 0x48, 0x4C, 0x32),
            packet.copyOfRange(0, 4),
        )
    }

    @Test
    fun `fields are laid out little endian at fixed offsets`() {
        val packet = WheelProtocol.encode(
            seq = 1192,
            steering = -0.75f,
            throttle = 0.5f,
            brake = 0.25f,
            clutch = 0f,
        )
        val buffer = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN)

        val magic = ByteArray(4)
        buffer.get(magic)
        assertEquals(4, buffer.position())
        assertEquals(1192, buffer.getInt())
        assertEquals(8, buffer.position())
        assertEquals(-0.75f, buffer.getFloat(), 1e-6f)
        assertEquals(12, buffer.position())
        assertEquals(0.5f, buffer.getFloat(), 1e-6f)
        assertEquals(16, buffer.position())
        assertEquals(0.25f, buffer.getFloat(), 1e-6f)
        assertEquals(20, buffer.position())
        assertEquals(0f, buffer.getFloat(), 1e-6f)
    }

    @Test
    fun `steering is clamped into minus one to one`() {
        val high = ByteBuffer.wrap(WheelProtocol.encode(0, 3.5f, 0f, 0f)).order(ByteOrder.LITTLE_ENDIAN)
        high.position(8)
        assertEquals(1f, high.getFloat(), 1e-6f)

        val low = ByteBuffer.wrap(WheelProtocol.encode(0, -42f, 0f, 0f)).order(ByteOrder.LITTLE_ENDIAN)
        low.position(8)
        assertEquals(-1f, low.getFloat(), 1e-6f)
    }

    @Test
    fun `pedal axes are clamped into zero to one`() {
        val high = ByteBuffer.wrap(WheelProtocol.encode(0, 0f, 5f, 9f)).order(ByteOrder.LITTLE_ENDIAN)
        high.position(12)
        assertEquals(1f, high.getFloat(), 1e-6f)
        assertEquals(1f, high.getFloat(), 1e-6f)

        val low = ByteBuffer.wrap(WheelProtocol.encode(0, 0f, -3f, -7f)).order(ByteOrder.LITTLE_ENDIAN)
        low.position(12)
        assertEquals(0f, low.getFloat(), 1e-6f)
        assertEquals(0f, low.getFloat(), 1e-6f)
    }

    @Test
    fun `encodeInto produces the same bytes as encode`() {
        val expected = WheelProtocol.encode(77, 0.25f, 0.8f, 0.1f)
        val actual = ByteArray(WheelProtocol.PACKET_SIZE)
        WheelProtocol.encodeInto(actual, 77, 0.25f, 0.8f, 0.1f)
        assertArrayEquals(expected, actual)
    }
}
