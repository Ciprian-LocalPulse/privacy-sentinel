package org.privacysentinel.app

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Foreground service that listens (through public Android APIs only) for
 *  - camera availability changes  -> "camera is in use by some app"
 *  - audio recording changes      -> "microphone is in use by some app"
 * and writes each start/stop to the local log.
 */
class MonitorService : Service() {

    private lateinit var store: EventStore
    private lateinit var cameraManager: CameraManager
    private lateinit var audioManager: AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private val io: ExecutorService = Executors.newSingleThreadExecutor()

    private val camerasInUse = HashSet<String>()
    private var micActive = false
    private var registered = false

    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraUnavailable(cameraId: String) {
            val wasIdle = camerasInUse.isEmpty()
            camerasInUse.add(cameraId)
            if (wasIdle) record(SensorKind.CAMERA, EventState.START)
        }

        override fun onCameraAvailable(cameraId: String) {
            if (camerasInUse.remove(cameraId) && camerasInUse.isEmpty()) {
                record(SensorKind.CAMERA, EventState.STOP)
            }
        }
    }

    private val audioCallback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>) {
            val active = configs.isNotEmpty()
            if (active && !micActive) {
                micActive = true
                record(SensorKind.MIC, EventState.START)
            } else if (!active && micActive) {
                micActive = false
                record(SensorKind.MIC, EventState.STOP)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannels(this)
        store = EventStore.get(this)
        cameraManager = getSystemService(CameraManager::class.java)
        audioManager = getSystemService(AudioManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, Notifier.ID_MONITOR, Notifier.monitorNotification(this), type)

        if (!registered) {
            registered = true
            // Do not log whatever is already running when we start; only track changes.
            micActive = audioManager.activeRecordingConfigurations.isNotEmpty()
            cameraManager.registerAvailabilityCallback(cameraCallback, handler)
            audioManager.registerAudioRecordingCallback(audioCallback, handler)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (registered) {
            cameraManager.unregisterAvailabilityCallback(cameraCallback)
            audioManager.unregisterAudioRecordingCallback(audioCallback)
            registered = false
        }
        io.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun record(sensor: String, state: String) {
        // Capture device state right now (main thread), do the heavy work off-thread.
        val screenOn = getSystemService(PowerManager::class.java).isInteractive
        val mode = audioManager.mode
        val inCall = mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION
        val start = state == EventState.START
        val suspicious = start && !screenOn && !(sensor == SensorKind.MIC && inCall)

        io.execute {
            val app = if (start && ForegroundAppResolver.hasUsageAccess(this)) {
                ForegroundAppResolver.probableForegroundApp(this)
            } else {
                null
            }
            store.insert(sensor, state, app, screenOn, suspicious)

            if (start && (suspicious || Prefs.alertAll(this))) {
                val title = when {
                    sensor == SensorKind.CAMERA && suspicious -> R.string.alert_suspicious_camera
                    sensor == SensorKind.CAMERA -> R.string.alert_use_camera
                    suspicious -> R.string.alert_suspicious_mic
                    else -> R.string.alert_use_mic
                }
                val text = if (app != null) {
                    getString(R.string.probable_app, ForegroundAppResolver.label(this, app))
                } else {
                    getString(R.string.alert_text_default)
                }
                Notifier.alert(this, getString(title), text)
            }
        }
    }
}
