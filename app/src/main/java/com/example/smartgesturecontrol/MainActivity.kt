package com.example.smartgesturecontrol

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartgesturecontrol.databinding.ActivityMainBinding
import com.example.smartgesturecontrol.model.DeviceRepository
import com.example.smartgesturecontrol.model.GestureType
import com.example.smartgesturecontrol.model.SmartDevice
import com.example.smartgesturecontrol.model.DeviceCommand
import com.example.smartgesturecontrol.sensor.GestureDetector

/**
 * MainActivity - Dashboard chính của ứng dụng Smart Gesture Control
 * Hiển thị tổng quan thiết bị, trạng thái kết nối, và cử chỉ gần đây
 *
 * Sử dụng Proximity Sensor để phát hiện vẫy tay (điện thoại nằm yên trên bàn)
 */
class MainActivity : AppCompatActivity(), SensorEventListener,
    GestureDetector.OnGestureDetectedListener,
    DeviceRepository.OnDeviceChangedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var proximitySensor: Sensor? = null  // Cảm biến tiệm cận
    private val gestureDetector = GestureDetector()
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        gestureDetector.listener = this
        DeviceRepository.addListener(this)

        setupUI()
        updateDeviceCards()
    }

    private fun setupUI() {
        // Navigation buttons
        binding.btnGestureControl.setOnClickListener {
            startActivity(Intent(this, GestureControlActivity::class.java))
        }
        binding.btnDeviceList.setOnClickListener {
            startActivity(Intent(this, DeviceListActivity::class.java))
        }
        binding.btnBluetooth.setOnClickListener {
            startActivity(Intent(this, BluetoothActivity::class.java))
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Device card clicks (toggle)
        binding.cardLight.setOnClickListener {
            DeviceRepository.toggleDevice("light_01")
        }
        binding.cardFan.setOnClickListener {
            DeviceRepository.toggleDevice("fan_01")
        }
        binding.cardTV.setOnClickListener {
            DeviceRepository.toggleDevice("tv_01")
        }
        binding.cardSpeaker.setOnClickListener {
            DeviceRepository.toggleDevice("speaker_01")
        }
    }

    private fun updateDeviceCards() {
        val devices = DeviceRepository.getDevices()
        for (device in devices) {
            updateSingleDeviceCard(device)
        }
    }

    private fun updateSingleDeviceCard(device: SmartDevice) {
        val statusText = if (device.isOn) "BẬT" else "TẮT"
        val statusColor = getColor(if (device.isOn) R.color.status_on else R.color.status_off)

        when (device.id) {
            "light_01" -> {
                binding.tvLightStatus.text = statusText
                binding.tvLightStatus.setTextColor(statusColor)
            }
            "fan_01" -> {
                binding.tvFanStatus.text = statusText
                binding.tvFanStatus.setTextColor(statusColor)
            }
            "tv_01" -> {
                binding.tvTVStatus.text = statusText
                binding.tvTVStatus.setTextColor(statusColor)
            }
            "speaker_01" -> {
                binding.tvSpeakerStatus.text = statusText
                binding.tvSpeakerStatus.setTextColor(statusColor)
            }
        }
    }

    // === SensorEventListener ===
    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> gestureDetector.onAccelerometerData(event.values)
            Sensor.TYPE_GYROSCOPE -> gestureDetector.onGyroscopeData(event.values)
            Sensor.TYPE_PROXIMITY -> {
                // Gửi dữ liệu proximity vào gesture detector
                // event.values[0] = khoảng cách (cm), thường là 0 (gần) hoặc max (xa)
                // event.sensor.maximumRange = khoảng cách tối đa của sensor
                gestureDetector.onProximityData(event.values[0], event.sensor.maximumRange)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // === GestureDetector callbacks ===
    override fun onGestureDetected(gesture: GestureType) {
        runOnUiThread {
            // Cập nhật UI
            binding.tvGestureEmoji.text = gesture.emoji
            binding.tvLastGesture.text = gesture.displayName
            binding.tvLastGestureTime.text = java.text.SimpleDateFormat("HH:mm:ss",
                java.util.Locale.getDefault()).format(java.util.Date())

            // Rung phản hồi
            vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))

            // Thực hiện hành động
            val command = DeviceRepository.executeGestureAction(gesture)
            if (command != null) {
                Toast.makeText(this, "${gesture.displayName}: ${command.action}", Toast.LENGTH_SHORT).show()
            }

            updateDeviceCards()
        }
    }

    override fun onSensorDataUpdated(accelData: FloatArray, gyroData: FloatArray) {}

    // === DeviceRepository callbacks ===
    override fun onDeviceStateChanged(device: SmartDevice) {
        runOnUiThread { updateSingleDeviceCard(device) }
    }

    override fun onCommandExecuted(command: DeviceCommand) {}

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        // Đăng ký Proximity Sensor - phát hiện vẫy tay
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        updateDeviceCards()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        DeviceRepository.removeListener(this)
    }
}
