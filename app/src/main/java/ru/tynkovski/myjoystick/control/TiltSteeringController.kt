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

/**
 * Turns phone tilt into a steering angle in degrees.
 *
 * The phone is held like a steering wheel and rotated in the plane of the screen. That rotation
 * is measured by a complementary filter:
 *  - the accelerometer provides the absolute angle of the gravity vector projected onto the
 *    screen plane (no drift, but noisy);
 *  - the gyroscope provides the fast rotation rate around the screen normal (smooth, but drifts).
 *
 * The neutral position is captured when the controller starts, so the wheel is centred with
 * whatever way the user naturally holds the phone.
 */
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
    private var lastTimestampNanos = 0L
    private var started = false

    /** False when the device has no accelerometer, so tilt steering cannot be used. */
    val isAvailable: Boolean get() = accelerometer != null

    fun start(maxAngleDegrees: Float) {
        this.maxAngleDegrees = maxAngleDegrees.coerceAtLeast(1f)
        if (started) return
        val accel = accelerometer ?: return
        started = true
        lastTimestampNanos = 0L
        hasNeutral = false
        filteredAngle = 0f
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        if (!started) return
        started = false
        lastTimestampNanos = 0L
        sensorManager.unregisterListener(this)
        onAngleChanged(0f)
    }

    fun updateMaxAngle(maxAngleDegrees: Float) {
        this.maxAngleDegrees = maxAngleDegrees.coerceAtLeast(1f)
    }

    /** Drops the current hold position as the new centre of the wheel. */
    fun recenter() {
        neutralAngle = filteredAngle
        hasNeutral = true
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!started) return
        val now = SystemClock.elapsedRealtimeNanos()
        val dt = if (lastTimestampNanos == 0L) {
            0f
        } else {
            ((now - lastTimestampNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
        }
        lastTimestampNanos = now

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val measured = deviceRollDegrees(event.values)
                if (!hasNeutral) {
                    filteredAngle = measured
                    neutralAngle = measured
                    hasNeutral = true
                } else {
                    // Accelerometer pulls the estimate towards the absolute gravity angle.
                    filteredAngle += accelWeight(dt) * shortestDelta(filteredAngle, measured)
                }
            }

            Sensor.TYPE_GYROSCOPE -> {
                // +Z of the device frame points out of the screen, so a positive rate is a
                // counter-clockwise rotation, the same direction the accelerometer angle uses.
                filteredAngle += event.values[2].toDegrees() * dt
            }
        }

        // The wheel angle used by the rest of the app grows clockwise on screen (it comes from
        // atan2 with the screen Y axis pointing down), while the roll estimate grows
        // counter-clockwise, so the sign has to be flipped here.
        val offset = if (hasNeutral) -shortestDelta(neutralAngle, filteredAngle) else 0f
        val angle = (offset * maxAngleDegrees / FULL_LOCK_DEGREES).coerceIn(
            -maxAngleDegrees,
            maxAngleDegrees,
        )
        onAngleChanged(angle)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /** Angle of the gravity vector inside the screen plane, corrected for display rotation. */
    private fun deviceRollDegrees(values: FloatArray): Float {
        val (right, up) = remapForRotation(values[0], values[1])
        if (sqrt(values[0] * values[0] + values[1] * values[1]) < MIN_GRAVITY_IN_PLANE) {
            // Phone is lying flat: in-plane rotation is undefined, keep the last estimate.
            return filteredAngle
        }
        return Math.toDegrees(atan2(right, up).toDouble()).toFloat()
    }

    /** Projects the device X/Y axes onto the current screen orientation. */
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

    /** Complementary filter weight for the accelerometer, based on the elapsed time. */
    private fun accelWeight(dt: Float): Float {
        if (dt <= 0f) return 0f
        return (1.0 - exp((-dt / ACCEL_TIME_CONSTANT_SECONDS).toDouble())).toFloat()
    }

    companion object {
        /** Steering travel used when no lock-to-lock value is provided. */
        const val DEFAULT_MAX_ANGLE = 480f

        /** Screen-plane rotation, in degrees, that corresponds to a full steering lock. */
        private const val FULL_LOCK_DEGREES = 40f

        /** Time constant of the accelerometer correction, seconds. */
        private const val ACCEL_TIME_CONSTANT_SECONDS = 0.25f

        /** Below this gravity magnitude (g) the phone is flat and the in-plane angle is noise. */
        private const val MIN_GRAVITY_IN_PLANE = 0.15f

        private fun Float.toDegrees(): Float = (this * 180.0 / Math.PI).toFloat()

        /** Signed shortest difference between two angles, in (-180, 180]. */
        private fun shortestDelta(from: Float, to: Float): Float {
            var delta = (to - from) % 360f
            if (delta > 180f) delta -= 360f
            if (delta < -180f) delta += 360f
            return if (abs(delta) < 1e-5f) 0f else delta
        }
    }
}
