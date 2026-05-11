package com.example.smartgesturecontrol

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.smartgesturecontrol.databinding.ActivityGestureControlBinding
import com.example.smartgesturecontrol.hardware.PhoneHardwareController
import com.example.smartgesturecontrol.model.DeviceRepository
import com.example.smartgesturecontrol.model.GestureType
import com.example.smartgesturecontrol.sensor.GestureDetector
import kotlin.math.max
import kotlin.math.min

/**
 * GestureControlActivity - Màn hình điều khiển và giám sát cử chỉ thời gian thực
 * Hiển thị Dashboard trực quan và gọi API hệ thống để điều khiển thiết bị
 */
class GestureControlActivity : AppCompatActivity(), SensorEventListener,
    GestureDetector.OnGestureDetectedListener {

    private lateinit var binding: ActivityGestureControlBinding
    private lateinit var sensorManager: SensorManager
    private lateinit var hardwareController: PhoneHardwareController
    
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var proximitySensor: Sensor? = null
    private val gestureDetector = GestureDetector()
    private var vibrator: Vibrator? = null
    private var isDetecting = false
    private val commandLogBuilder = StringBuilder()
    
    // Giả lập trạng thái bài hát trên UI
    private var currentSongIndex = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGestureControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        hardwareController = PhoneHardwareController(this)
        
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        gestureDetector.listener = this

        loadSettings()
        initDashboardUI()

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnToggleGesture.setOnClickListener {
            isDetecting = !isDetecting
            if (isDetecting) {
                startDetecting()
            } else {
                stopDetecting()
            }
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("gesture_settings", Context.MODE_PRIVATE)
        gestureDetector.tiltThreshold = prefs.getFloat("tilt_threshold", 5f)
        gestureDetector.rotateThreshold = prefs.getFloat("rotate_threshold", 2.5f)
    }

    private fun initDashboardUI() {
        // Đồng bộ UI với trạng thái phần cứng hiện tại
        updateLightUI(hardwareController.getFlashlightState())
        updateVolumeUI(hardwareController.getVolumePercentage())
        updateMediaUI()
    }

    private fun startDetecting() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        binding.btnToggleGesture.text = "Dừng nhận diện"
        binding.tvDashboardStatus.text = "Đang quét cử chỉ..."
        binding.tvDashboardRecognized.text = "✅ Đã sẵn sàng"
    }

    private fun stopDetecting() {
        sensorManager.unregisterListener(this)
        gestureDetector.reset()
        binding.btnToggleGesture.text = "Bắt đầu nhận diện"
        binding.tvDashboardStatus.text = "Đã tạm dừng"
        resetSensorDisplay()
    }

    private fun resetSensorDisplay() {
        binding.pbAx.progress = 150
        binding.pbAy.progress = 150
        binding.pbGz.progress = 100
        binding.tvAxVal.text = "0.0 m/s²"
        binding.tvAyVal.text = "0.0 m/s²"
        binding.tvGzVal.text = "0.0 rad/s"
    }

    // === SensorEventListener ===
    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> gestureDetector.onAccelerometerData(event.values)
            Sensor.TYPE_GYROSCOPE -> gestureDetector.onGyroscopeData(event.values)
            Sensor.TYPE_PROXIMITY -> {
                gestureDetector.onProximityData(event.values[0], event.sensor.maximumRange)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // === GestureDetector callbacks ===
    override fun onGestureDetected(gesture: GestureType) {
        runOnUiThread {
            binding.tvDashboardEmoji.text = gesture.emoji
            binding.tvDashboardRecognized.text = "✅ Nhận diện: ${gesture.displayName}"

            val prefs = getSharedPreferences("gesture_settings", Context.MODE_PRIVATE)
            if (prefs.getBoolean("vibration_enabled", true)) {
                vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            }

            // 1. Thực hiện lệnh trên phần cứng điện thoại (Đèn flash, âm lượng máy)
            executeNativeHardwareAction(gesture)
            
            // 2. ĐỒNG THỜI: Gửi lệnh ra thiết bị IoT thật qua Bluetooth/WiFi (nếu có kết nối)
            DeviceRepository.executeGestureAction(gesture)
        }
    }
    
    private fun executeNativeHardwareAction(gesture: GestureType) {
        when (gesture) {
            GestureType.SHAKE -> {
                val isOn = hardwareController.toggleFlashlight()
                updateLightUI(isOn)
                logCommand("Flashlight", if (isOn) "ON" else "OFF")
            }
            GestureType.TILT_UP -> {
                val vol = hardwareController.adjustVolume(increase = true)
                updateVolumeUI(vol)
                logCommand("Media Volume", "UP ($vol%)")
            }
            GestureType.TILT_DOWN -> {
                val vol = hardwareController.adjustVolume(increase = false)
                updateVolumeUI(vol)
                logCommand("Media Volume", "DOWN ($vol%)")
            }
            GestureType.ROTATE_LEFT -> {
                hardwareController.changeSong(next = false)
                if (currentSongIndex > 1) currentSongIndex--
                updateMediaUI()
                logCommand("Media Player", "PREVIOUS")
            }
            GestureType.ROTATE_RIGHT -> {
                hardwareController.changeSong(next = true)
                currentSongIndex++
                updateMediaUI()
                logCommand("Media Player", "NEXT")
            }
            else -> {
                // Không làm gì với các cử chỉ khác (ví dụ: FLIP)
            }
        }
    }
    
    private fun logCommand(target: String, action: String) {
        commandLogBuilder.appendLine("[$target] -> $action")
        binding.tvCommandLog.text = commandLogBuilder.toString()
    }

    // === Cập nhật UI cụ thể ===
    private fun updateLightUI(isOn: Boolean) {
        if (isOn) {
            binding.tvLightState.text = "[ BẬT ● ]"
            binding.tvLightState.setBackgroundResource(R.drawable.bg_label_on)
            binding.tvLightState.setTextColor(ContextCompat.getColor(this, R.color.on_primary))
        } else {
            binding.tvLightState.text = "[ TẮT ● ]"
            binding.tvLightState.setBackgroundResource(R.drawable.bg_label_off)
            binding.tvLightState.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant))
        }
    }
    
    private fun updateVolumeUI(volumePercent: Int) {
        binding.pbVolume.progress = volumePercent
        binding.tvVolumeVal.text = "$volumePercent%"
    }
    
    private fun updateMediaUI() {
        binding.tvSongName.text = "Bài $currentSongIndex ►"
    }

    override fun onSensorDataUpdated(accelData: FloatArray, gyroData: FloatArray) {
        runOnUiThread {
            binding.tvAxVal.text = String.format("%.1f m/s²", accelData[0])
            binding.tvAyVal.text = String.format("%.1f m/s²", accelData[1])
            binding.tvGzVal.text = String.format("%.1f rad/s", gyroData[2])

            binding.pbAx.progress = max(0, min(300, (accelData[0] * 10 + 150).toInt()))
            binding.pbAy.progress = max(0, min(300, (accelData[1] * 10 + 150).toInt()))
            binding.pbGz.progress = max(0, min(200, (gyroData[2] * 10 + 100).toInt()))
        }
    }

    override fun onPause() {
        super.onPause()
        if (isDetecting) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onResume() {
        super.onResume()
        if (isDetecting) {
            startDetecting()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}
