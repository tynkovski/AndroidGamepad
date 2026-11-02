package ru.tynkovski.myjoystick.wheel

/**
 * How the steering axis is driven.
 */
enum class ControlMode {
    /** Steering wheel is rotated by dragging the on-screen rim. */
    WHEEL,

    /** Steering follows the phone tilt (accelerometer + gyroscope). */
    TILT,
}
