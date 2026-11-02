package ru.tynkovski.myjoystick.control

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import ru.tynkovski.myjoystick.wheel.WheelInputState

@Stable
class ControlInputState {

    var wheelAngleDegrees by mutableFloatStateOf(0f)
        private set

    var throttle by mutableFloatStateOf(0f)
        private set

    var brake by mutableFloatStateOf(0f)
        private set

    private fun halfTravel(lockToLockDegrees: Int): Float =
        (lockToLockDegrees.coerceAtLeast(2) / 2).toFloat()

    fun steering(lockToLockDegrees: Int): Float {
        val half = halfTravel(lockToLockDegrees)
        return (wheelAngleDegrees / half).coerceIn(-1f, 1f)
    }

    fun rotateWheel(deltaDegrees: Float, lockToLockDegrees: Int) {
        val half = halfTravel(lockToLockDegrees)
        wheelAngleDegrees = (wheelAngleDegrees + deltaDegrees).coerceIn(-half, half)
        WheelInputState.steering = steering(lockToLockDegrees)
    }

    /** Applies an absolute steering angle, e.g. from the tilt sensor. */
    fun setWheelAngle(angleDegrees: Float, lockToLockDegrees: Int) {
        val half = halfTravel(lockToLockDegrees)
        wheelAngleDegrees = angleDegrees.coerceIn(-half, half)
        WheelInputState.steering = steering(lockToLockDegrees)
    }

    fun updateThrottle(value: Float) {
        throttle = value.coerceIn(0f, 1f)
        WheelInputState.throttle = throttle
    }

    fun updateBrake(value: Float) {
        brake = value.coerceIn(0f, 1f)
        WheelInputState.brake = brake
    }

    fun refreshSteering(lockToLockDegrees: Int) {
        val half = halfTravel(lockToLockDegrees)
        wheelAngleDegrees = wheelAngleDegrees.coerceIn(-half, half)
        WheelInputState.steering = steering(lockToLockDegrees)
    }
}
