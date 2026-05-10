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
import com.example.smartgesturecontrol.databinding.ActivityGestureControlBinding
import com.example.smartgesturecontrol.model.DeviceRepository
import com.example.smartgesturecontrol.model.GestureType
import com.example.smartgesturecontrol.sensor.GestureDetector

/**
 * GestureControlActivity - Màn hình điều khiển và giám sát cử chỉ thời gian thực
 * Hiển thị dữ liệu sensor, cử chỉ được phát hiện, và nhật ký lệnh
 */
class GestureControlActivity : AppCompatActivity(), SensorEventListener,
    GestureDetector.OnGestureDetectedListener {

    private lateinit var binding: ActivityGestureControlBinding
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var proximitySensor: Sensor? = null  // Cảm biến tiệm cận cho vẫy tay
    private val gestureDetector = GestureDetector()
    private var vibrator: Vibrator? = null
    private var isDetecting = false
    private val commandLogBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGestureControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        gestureDetector.listener = this

        // Load saved thresholds
        loadSettings()

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnToggleGesture.setOnClickListener {
            isDetecting = !isDetecting
            if (isDetecting) {
                startDetecting()
            } else {
                stopDetecting()
            }
        }

        // Hiển thị log cũ
        val existingLog = DeviceRepository.getCommandLog()
        if (existingLog.isNotEmpty()) {
            commandLogBuilder.clear()
            existingLog.takeLast(20).forEach {
                commandLogBuilder.appendLine(it.toString())
            }
            binding.tvCommandLog.text = commandLogBuilder.toString()
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("gesture_settings", Context.MODE_PRIVATE)
        gestureDetector.tiltThreshold = prefs.getFloat("tilt_threshold", 5f)
        gestureDetector.rotateThreshold = prefs.getFloat("rotate_threshold", 2.5f)
    }

    private fun startDetecting() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        // Đăng ký Proximity Sensor để phát hiện vẫy tay
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        binding.btnToggleGesture.text = "Dừng nhận diện"
        binding.tvGestureName.text = "Đang nhận diện..."
        binding.tvGestureAction.text = "Vẫy tay trước điện thoại hoặc nghiêng/xoay"
    }

    private fun stopDetecting() {
        sensorManager.unregisterListener(this)
        gestureDetector.reset()
        binding.btnToggleGesture.text = "Bắt đầu nhận diện"
        binding.tvGestureName.text = "Đã dừng"
        binding.tvGestureAction.text = "Nhấn nút để bắt đầu lại"
        resetSensorDisplay()
    }

    private fun resetSensorDisplay() {
        binding.tvAccelX.text = "X: 0.00"
        binding.tvAccelY.text = "Y: 0.00"
        binding.tvAccelZ.text = "Z: 0.00"
        binding.tvGyroX.text = "X: 0.00"
        binding.tvGyroY.text = "Y: 0.00"
        binding.tvGyroZ.text = "Z: 0.00"
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
            binding.tvGestureEmoji.text = gesture.emoji
            binding.tvGestureName.text = gesture.displayName

            val actionText = when (gesture) {
                GestureType.SHAKE -> "→ Bật/Tắt đèn"
                GestureType.TILT_UP -> "→ Tăng âm lượng"
                GestureType.TILT_DOWN -> "→ Giảm âm lượng"
                GestureType.ROTATE_LEFT -> "→ Bài trước"
                GestureType.ROTATE_RIGHT -> "→ Bài tiếp theo"
                GestureType.FLIP -> "→ Bật/Tắt quạt"
            }
            binding.tvGestureAction.text = actionText

            // Rung phản hồi
            val prefs = getSharedPreferences("gesture_settings", Context.MODE_PRIVATE)
            if (prefs.getBoolean("vibration_enabled", true)) {
                vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            }

            // Thực hiện hành động và ghi log
            val command = DeviceRepository.executeGestureAction(gesture)
            if (command != null) {
                commandLogBuilder.appendLine(command.toString())
                binding.tvCommandLog.text = commandLogBuilder.toString()
            }
        }
    }

    override fun onSensorDataUpdated(accelData: FloatArray, gyroData: FloatArray) {
        runOnUiThread {
            binding.tvAccelX.text = String.format("X: %.2f", accelData[0])
            binding.tvAccelY.text = String.format("Y: %.2f", accelData[1])
            binding.tvAccelZ.text = String.format("Z: %.2f", accelData[2])
            binding.tvGyroX.text = String.format("X: %.2f", gyroData[0])
            binding.tvGyroY.text = String.format("Y: %.2f", gyroData[1])
            binding.tvGyroZ.text = String.format("Z: %.2f", gyroData[2])
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
