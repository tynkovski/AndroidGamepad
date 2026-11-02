package ru.tynkovski.myjoystick.wheel

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object EventSender {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun send(settings: WheelSettings, event: WheelEvent) {
        val host = settings.ip
        if (host.isBlank()) return
        val port = settings.eventPort

        scope.launch {
            runCatching {
                val address = InetAddress.getByName(host)
                DatagramSocket().use { socket ->
                    socket.send(
                        DatagramPacket(event.payload, EventProtocol.PACKET_SIZE, address, port)
                    )
                }
            }.onFailure {
                Log.w(TAG, "Failed to send $event to $host:$port", it)
            }
        }
    }

    private const val TAG = "EventSender"
}
