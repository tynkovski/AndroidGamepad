package ru.tynkovski.myjoystick.wheel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.tynkovski.myjoystick.MainActivity
import ru.tynkovski.myjoystick.R
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.time.Duration.Companion.milliseconds

class WheelService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var sendJob: Job? = null

    @Volatile
    private var socket: DatagramSocket? = null

    private var wakeLock: PowerManager.WakeLock? = null

    @Volatile
    private var session = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopStreaming()
            return START_NOT_STICKY
        }

        val settings = WheelConfig.current

        goForeground(
            if (settings.ip.isBlank()) getString(R.string.notification_waiting)
            else getString(R.string.status_streaming, settings.target)
        )

        if (settings.ip.isBlank()) {
            WheelState.update { it.copy(error = getString(R.string.error_no_ip)) }
            stopStreaming()
            return START_NOT_STICKY
        }

        acquireWakeLock()
        startStreaming(settings)
        return START_STICKY
    }

    private fun startStreaming(settings: WheelSettings) {
        val currentSession = ++session
        sendJob?.cancel()
        sendJob = null
        closeSocket()

        val target = settings.target
        WheelState.update {
            it.copy(isRunning = true, target = target, packetsSent = 0L, error = null)
        }

        sendJob = scope.launch {
            var sessionSocket: DatagramSocket? = null
            try {
                val address = InetAddress.getByName(settings.ip)
                val datagramSocket = DatagramSocket().also {
                    sessionSocket = it
                    socket = it
                }
                val packet = ByteArray(WheelProtocol.PACKET_SIZE)
                val outbound = DatagramPacket(packet, packet.size, address, settings.port)
                val intervalMs = 1000L / WheelProtocol.SEND_HZ

                var sequence = 0
                var packets = 0L
                var nextTick = System.currentTimeMillis()
                var lastPublish = 0L

                while (isActive) {
                    WheelProtocol.encodeInto(
                        target = packet,
                        seq = sequence,
                        steering = WheelInputState.steering,
                        throttle = WheelInputState.throttle,
                        brake = WheelInputState.brake,
                    )
                    datagramSocket.send(outbound)

                    sequence = (sequence + 1) and WheelProtocol.SEQ_WRAP
                    packets++

                    val now = System.currentTimeMillis()
                    if (now - lastPublish >= UI_PUBLISH_INTERVAL_MS) {
                        val sent = packets
                        WheelState.update { it.copy(packetsSent = sent) }
                        lastPublish = now
                        acquireWakeLock()
                    }

                    nextTick += intervalMs
                    val sleep = nextTick - System.currentTimeMillis()
                    if (sleep > 0) {
                        delay(sleep.milliseconds)
                    } else {
                        nextTick = System.currentTimeMillis()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (currentSession == session) {
                    Log.e(TAG, "UDP send loop failed", e)
                    WheelState.update {
                        it.copy(
                            isRunning = false,
                            error = sendErrorText(e),
                        )
                    }
                    stopStreaming()
                }
            } finally {
                sessionSocket?.let { own ->
                    if (socket === own) socket = null
                    own.runCatching { close() }
                }
            }
        }
    }

    private fun sendErrorText(e: Exception): String {
        val message = e.message.orEmpty()
        return if (message.contains("EPERM")) {
            getString(R.string.error_local_network)
        } else {
            e.message ?: getString(R.string.error_send_failed)
        }
    }

    private fun stopStreaming() {
        session++
        sendJob?.cancel()
        sendJob = null
        closeSocket()
        releaseWakeLock()
        WheelState.update { it.copy(isRunning = false) }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun closeSocket() {
        socket?.runCatching { close() }
        socket = null
    }

    override fun onDestroy() {
        session++
        sendJob?.cancel()
        closeSocket()
        releaseWakeLock()
        WheelState.update { it.copy(isRunning = false) }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:wheel")
            .apply {
                setReferenceCounted(false)
                acquire(WAKE_LOCK_TIMEOUT_MS)
            }
    }

    private fun releaseWakeLock() {
        wakeLock?.runCatching { if (isHeld) release() }
        wakeLock = null
    }

    private fun goForeground(text: String) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(text), type)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.channel_description)
            setShowBadge(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            REQUEST_OPEN,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this,
            REQUEST_STOP,
            Intent(this, WheelService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(openApp)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notification_stop),
                stop,
            )
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        private const val TAG = "WheelService"
        private const val CHANNEL_ID = "wheel_stream"
        private const val NOTIFICATION_ID = 42
        private const val REQUEST_OPEN = 0
        private const val REQUEST_STOP = 1
        private const val UI_PUBLISH_INTERVAL_MS = 100L
        private const val WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L

        private const val ACTION_STOP = "ru.tynkovski.myjoystick.action.STOP"

        fun start(context: Context) {
            val intent = Intent(context, WheelService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WheelService::class.java))
        }
    }
}
