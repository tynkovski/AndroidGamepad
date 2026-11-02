package ru.tynkovski.myjoystick.control

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.view.Surface
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.sqrt

class TiltSteeringController(
    context: Context,
    private val onAngleChanged: (Float) -> Unit,
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

    private var maxAngleDegrees = DEFAULT_MAX_ANGLE
    private var filteredAngle = 0f
    private var neutralAngle = 0f
    private var hasNeutral = false
    private var started = false

    private var accelTimeConstant = FAST_ACCEL_TIME_CONSTANT_SECONDS

    private var lastAccelNanos = 0L
    private var lastGyroNanos = 0L

    private var emittedAngle = Float.NaN

    val isAvailable: Boolean get() = accelerometer != null

    fun start(maxAngleDegrees: Float) {
        this.maxAngleDegrees = maxAngleDegrees.coerceAtLeast(1f)
        if (started) return
        val accel = accelerometer ?: return
        started = true
        hasNeutral = false
        filteredAngle = 0f
        neutralAngle = 0f
        lastAccelNanos = 0L
        lastGyroNanos = 0L
        emittedAngle = Float.NaN
        accelTimeConstant = if (gyroscope != null) {
            SLOW_ACCEL_TIME_CONSTANT_SECONDS
        } else {
            FAST_ACCEL_TIME_CONSTANT_SECONDS
        }
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        if (!started) return
        started = false
        hasNeutral = false
        lastAccelNanos = 0L
        lastGyroNanos = 0L
        sensorManager.unregisterListener(this)
        onAngleChanged(0f)
    }

    fun updateMaxAngle(maxAngleDegrees: Float) {
        this.maxAngleDegrees = maxAngleDegrees.coerceAtLeast(1f)
    }

    fun recenter() {
        if (!hasNeutral) return
        neutralAngle = filteredAngle
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!started) return
        val changed = when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> updateFromAccelerometer(event.values)
            Sensor.TYPE_GYROSCOPE -> updateFromGyroscope(event.values[2])
            else -> false
        }
        if (changed) emitAngle()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun updateFromAccelerometer(values: FloatArray): Boolean {
        val now = SystemClock.elapsedRealtimeNanos()
        val dt = elapsedSeconds(lastAccelNanos, now)
        lastAccelNanos = now

        val measured = deviceRollDegrees(values) ?: return false

        if (!hasNeutral) {
            filteredAngle = measured
            neutralAngle = measured
            hasNeutral = true
        } else {
            filteredAngle += accelWeight(dt) * shortestDelta(filteredAngle, measured)
        }
        return true
    }

    private fun updateFromGyroscope(rateRad: Float): Boolean {
        if (!hasNeutral) return false
        val now = SystemClock.elapsedRealtimeNanos()
        val dt = elapsedSeconds(lastGyroNanos, now)
        lastGyroNanos = now

        filteredAngle += gyroRate(rateRad) * dt
        return true
    }

    private fun emitAngle() {
        if (!hasNeutral) return
        val offset = -shortestDelta(neutralAngle, filteredAngle)
        val angle = (offset * maxAngleDegrees / FULL_LOCK_DEGREES).coerceIn(
            -maxAngleDegrees,
            maxAngleDegrees,
        )
        if (!emittedAngle.isNaN() && abs(angle - emittedAngle) < ANGLE_EMIT_EPSILON) return
        emittedAngle = angle
        onAngleChanged(angle)
    }

    private fun deviceRollDegrees(values: FloatArray): Float? {
        val inPlane = sqrt(values[0] * values[0] + values[1] * values[1])
        if (inPlane < MIN_GRAVITY_IN_PLANE) return null
        val (right, up) = remapForRotation(values[0], values[1])
        return Math.toDegrees(atan2(right, up).toDouble()).toFloat()
    }

    private fun remapForRotation(x: Float, y: Float): Pair<Float, Float> =
        when (displayRotation()) {
            Surface.ROTATION_90 -> -y to x
            Surface.ROTATION_180 -> -x to -y
            Surface.ROTATION_270 -> y to -x
            else -> x to y
        }

    private fun displayRotation(): Int = runCatching {
        @Suppress("DEPRECATION")
        windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_0
    }.getOrDefault(Surface.ROTATION_0)

    private fun elapsedSeconds(previousNanos: Long, nowNanos: Long): Float {
        if (previousNanos == 0L) return 0f
        return ((nowNanos - previousNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
    }

    private fun accelWeight(dt: Float): Float {
        if (dt <= 0f) return 0f
        return (1.0 - exp((-dt / accelTimeConstant).toDouble())).toFloat()
    }

    private fun gyroRate(rawRateRad: Float): Float {
        val degrees = rawRateRad.toDegrees()
        return if (abs(degrees) < GYRO_RATE_DEADBAND) 0f else degrees
    }

    companion object {
        const val DEFAULT_MAX_ANGLE = 480f

        private const val FULL_LOCK_DEGREES = 40f

        private const val SLOW_ACCEL_TIME_CONSTANT_SECONDS = 1.5f

        private const val FAST_ACCEL_TIME_CONSTANT_SECONDS = 0.2f

        private const val GYRO_RATE_DEADBAND = 0.6f

        private const val ANGLE_EMIT_EPSILON = 0.05f

        private const val MIN_GRAVITY_IN_PLANE = 0.15f

        private fun Float.toDegrees(): Float = (this * 180.0 / Math.PI).toFloat()

        private fun shortestDelta(from: Float, to: Float): Float {
            var delta = (to - from) % 360f
            if (delta > 180f) delta -= 360f
            if (delta < -180f) delta += 360f
            return if (abs(delta) < 1e-5f) 0f else delta
        }
    }
}
