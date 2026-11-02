package ru.tynkovski.myjoystick

import android.app.Application
import ru.tynkovski.myjoystick.wheel.WheelConfig

class MyJoystickApp : Application() {

    override fun onCreate() {
        super.onCreate()
        WheelConfig.init(this)
    }
}
