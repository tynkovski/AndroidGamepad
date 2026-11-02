package ru.tynkovski.myjoystick

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.tynkovski.myjoystick.control.ControlInputState
import ru.tynkovski.myjoystick.control.ControlScreen
import ru.tynkovski.myjoystick.settings.SettingsScreen
import ru.tynkovski.myjoystick.ui.theme.MyJoystickTheme
import ru.tynkovski.myjoystick.wheel.EventSender
import ru.tynkovski.myjoystick.wheel.WheelConfig
import ru.tynkovski.myjoystick.wheel.WheelService
import ru.tynkovski.myjoystick.wheel.WheelState

class MainActivity : ComponentActivity() {

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val requestLocalNetwork =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted && WheelState.status.value.isRunning) {
                WheelService.start(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        requestNotificationPermissionIfNeeded()
        requestLocalNetworkPermissionIfNeeded()

        setContent {
            MyJoystickTheme(darkTheme = true, dynamicColor = false) {
                val settings by WheelConfig.settings.collectAsStateWithLifecycle()
                val status by WheelState.status.collectAsStateWithLifecycle()
                val controlState = remember { ControlInputState() }
                var showSettings by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(settings.lockToLockDegrees) {
                    controlState.refreshSteering(settings.lockToLockDegrees)
                }

                if (showSettings) {
                    SettingsScreen(
                        initial = settings,
                        onSave = { updated ->
                            val wasStreaming = status.isRunning
                            WheelConfig.save(updated)
                            controlState.refreshSteering(updated.lockToLockDegrees)
                            showSettings = false
                            if (wasStreaming) {
                                if (updated.ip.isNotBlank()) {
                                    WheelService.start(this)
                                } else {
                                    WheelService.stop(this)
                                }
                            }
                        },
                        onBack = { showSettings = false },
                        onConnect = { pending ->
                            WheelConfig.save(pending)
                            WheelService.start(this)
                        },
                        onDisconnect = { WheelService.stop(this) },
                    )
                } else {
                    ControlScreen(
                        state = controlState,
                        lockToLockDegrees = settings.lockToLockDegrees,
                        pressRampMs = settings.pressRampMs,
                        releaseRampMs = settings.releaseRampMs,
                        brakeAxisInverted = settings.brakeAxisInverted,
                        eventsEnabled = status.isRunning,
                        onEvent = { event -> EventSender.send(settings, event) },
                        onOpenSettings = { showSettings = true },
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    @SuppressLint("InlinedApi")
    private fun requestLocalNetworkPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) return
        val permission = Manifest.permission.ACCESS_LOCAL_NETWORK
        val granted = ContextCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) requestLocalNetwork.launch(permission)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
