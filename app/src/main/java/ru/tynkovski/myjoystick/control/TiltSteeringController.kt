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
    private var sensitivity = DEFAULT_SENSITIVITY

    private var roll = 0f
    private var neutralRoll = 0f
    private var hasNeutral = false
    private var lyingFlat = false
    private var anchored = true
    private var started = false

    private var rateDeg = 0f
    private var gyroBiasDeg = 0f
    private var restSeconds = 0f

    private var accelTimeConstant = ACCEL_TIME_CONSTANT_SECONDS

    private var lastAccelNanos = 0L
    private var lastGyroNanos = 0L

    private var emittedAngle = Float.NaN

    val isAvailable: Boolean get() = accelerometer != null

    fun start(maxAngleDegrees: Float, sensitivity: Float = DEFAULT_SENSITIVITY) {
        this.maxAngleDegrees = maxAngleDegrees.coerceAtLeast(1f)
        updateSensitivity(sensitivity)
        if (started) return
        val accel = accelerometer ?: return
        started = true
        hasNeutral = false
        lyingFlat = false
        anchored = true
        roll = 0f
        neutralRoll = 0f
        rateDeg = 0f
        restSeconds = 0f
        lastAccelNanos = 0L
        lastGyroNanos = 0L
        emittedAngle = Float.NaN
        accelTimeConstant = if (gyroscope != null) {
            ACCEL_TIME_CONSTANT_SECONDS
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
        lyingFlat = false
        anchored = true
        rateDeg = 0f
        restSeconds = 0f
        lastAccelNanos = 0L
        lastGyroNanos = 0L
        sensorManager.unregisterListener(this)
        onAngleChanged(0f)
    }

    fun updateMaxAngle(maxAngleDegrees: Float) {
        this.maxAngleDegrees = maxAngleDegrees.coerceAtLeast(1f)
    }

    fun updateSensitivity(sensitivity: Float) {
        this.sensitivity = sensitivity.coerceIn(MIN_SENSITIVITY, MAX_SENSITIVITY)
    }

    fun recenter() {
        if (!hasNeutral) return
        neutralRoll = roll
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

        val reading = tiltReading(values) ?: return false

        if (reading.confidence <= 0f) {
            lyingFlat = true
            return false
        }

        if (lyingFlat) {
            lyingFlat = false
            if (hasNeutral) {
                val offset = shortestDelta(roll, neutralRoll)
                neutralRoll = wrapDegrees(reading.roll + offset)
            }
            roll = reading.roll
            return true
        }

        if (!hasNeutral) {
            roll = reading.roll
            neutralRoll = reading.roll
            hasNeutral = true
            return true
        }

        if (abs(rateDeg) > ACCEL_ANCHOR_MAX_RATE) {
            anchored = false
            restSeconds = 0f
            return false
        }
        if (!anchored) {
            if (abs(rateDeg) > ACCEL_ANCHOR_RESUME_RATE) return false
            anchored = true
        }

        val weight = accelWeight(dt) * reading.confidence
        val correction = weight * shortestDelta(roll, reading.roll)
        roll = wrapDegrees(roll + correction)
        return true
    }

    private fun updateFromGyroscope(rateRad: Float): Boolean {
        val now = SystemClock.elapsedRealtimeNanos()
        val dt = elapsedSeconds(lastGyroNanos, now)
        lastGyroNanos = now
        if (dt <= 0f) return false

        val rawRateDeg = rateRad.toDegrees()
        updateGyroBias(rawRateDeg, dt)
        rateDeg = rawRateDeg - gyroBiasDeg

        if (!hasNeutral) {
            if (!lyingFlat) return false
            hasNeutral = true
        }

        val rate = deadband(rateDeg)
        if (rate == 0f) return false
        roll = wrapDegrees(roll + (rate * dt))
        return true
    }

    private fun updateGyroBias(rawRateDeg: Float, dt: Float) {
        if (abs(rawRateDeg) > GYRO_BIAS_REST_MAX_RATE) {
            restSeconds = 0f
            return
        }
        restSeconds += dt
        if (restSeconds < GYRO_BIAS_REST_MIN_SECONDS) return
        val weight = (1.0 - exp((-dt / GYRO_BIAS_TIME_CONSTANT_SECONDS).toDouble())).toFloat()
        val step = weight * (rawRateDeg - gyroBiasDeg)
        gyroBiasDeg = (gyroBiasDeg + step).coerceIn(-MAX_GYRO_BIAS_RATE, MAX_GYRO_BIAS_RATE)
    }

    private fun emitAngle() {
        if (!hasNeutral) return
        val offset = shortestDelta(roll, neutralRoll)
        val gain = (maxAngleDegrees * sensitivity) / FULL_LOCK_DEGREES
        val angle = (offset * gain).coerceIn(-maxAngleDegrees, maxAngleDegrees)
        val isUnchanged = !emittedAngle.isNaN() &&
            (abs(angle - emittedAngle) < ANGLE_EMIT_EPSILON)
        if (isUnchanged) return
        emittedAngle = angle
        onAngleChanged(angle)
    }

    private fun tiltReading(values: FloatArray): TiltReading? {
        val x = values[0]
        val y = values[1]
        val z = values[2]
        val magnitude = sqrt((x * x) + (y * y) + (z * z))

        if (abs(magnitude - GRAVITY_EARTH) > GRAVITY_TOLERANCE) return null

        val flatness = abs(z) / magnitude
        val trustedFlatness = if (lyingFlat) {
            ACCEL_RECOVER_FLATNESS
        } else {
            ACCEL_TRUSTED_FLATNESS
        }
        val confidence = when {
            flatness <= trustedFlatness -> 1f
            flatness >= ACCEL_IGNORED_FLATNESS -> 0f
            else -> (ACCEL_IGNORED_FLATNESS - flatness) /
                (ACCEL_IGNORED_FLATNESS - trustedFlatness)
        }
        if (confidence <= 0f) return TiltReading(0f, 0f)

        val (right, up) = remapForRotation(x, y)
        val roll = Math.toDegrees(atan2(right, up).toDouble()).toFloat()
        return TiltReading(roll, confidence)
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

    private fun deadband(rateDeg: Float): Float =
        if (abs(rateDeg) < GYRO_RATE_DEADBAND) 0f else rateDeg

    private class TiltReading(val roll: Float, val confidence: Float)

    companion object {
        const val DEFAULT_MAX_ANGLE = 480f

        private const val DEFAULT_SENSITIVITY = 1f

        private const val MIN_SENSITIVITY = 0.1f

        private const val MAX_SENSITIVITY = 1f

        private const val FULL_LOCK_DEGREES = 40f

        private const val ACCEL_TIME_CONSTANT_SECONDS = 0.35f

        private const val FAST_ACCEL_TIME_CONSTANT_SECONDS = 0.15f

        private const val ACCEL_ANCHOR_MAX_RATE = 5f

        private const val ACCEL_ANCHOR_RESUME_RATE = 2.5f

        private const val GYRO_RATE_DEADBAND = 0.05f

        private const val GYRO_BIAS_REST_MAX_RATE = 0.6f

        private const val GYRO_BIAS_REST_MIN_SECONDS = 1.2f

        private const val GYRO_BIAS_TIME_CONSTANT_SECONDS = 6f

        private const val MAX_GYRO_BIAS_RATE = 2f

        private const val ANGLE_EMIT_EPSILON = 0.05f

        private const val GRAVITY_EARTH = 9.80665f

        private const val GRAVITY_TOLERANCE = 1.5f

        private const val ACCEL_TRUSTED_FLATNESS = 0.93f

        private const val ACCEL_RECOVER_FLATNESS = 0.90f

        private const val ACCEL_IGNORED_FLATNESS = 0.99f

        private fun Float.toDegrees(): Float = ((this * 180.0) / Math.PI).toFloat()

        private fun shortestDelta(from: Float, to: Float): Float {
            var delta = (to - from) % 360f
            if (delta > 180f) delta -= 360f
            if (delta < -180f) delta += 360f
            return if (abs(delta) < 1e-5f) 0f else delta
        }

        private fun wrapDegrees(value: Float): Float = shortestDelta(0f, value)
    }
}
