package ru.tynkovski.myjoystick.wheel

object WheelInputState {

    @Volatile
    var steering: Float = 0f

    @Volatile
    var throttle: Float = 0f

    @Volatile
    var brake: Float = 0f

    fun reset() {
        steering = 0f
        throttle = 0f
        brake = 0f
    }
}
