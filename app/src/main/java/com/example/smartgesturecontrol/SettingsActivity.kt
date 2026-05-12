package com.example.smartgesturecontrol

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartgesturecontrol.databinding.ActivitySettingsBinding
import com.example.smartgesturecontrol.model.DeviceRepository

/**
 * SettingsActivity - Cài đặt ngưỡng nhạy cử chỉ và tùy chọn ứng dụng
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        loadSettings()
        setupSliders()
        setupButtons()
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("gesture_settings", Context.MODE_PRIVATE)
        val tilt = prefs.getFloat("tilt_threshold", 5f)
        val rotate = prefs.getFloat("rotate_threshold", 2.5f)
        val vibration = prefs.getBoolean("vibration_enabled", true)

        binding.sliderTilt.value = tilt
        binding.sliderRotate.value = rotate
        binding.switchVibration.isChecked = vibration

        binding.tvTiltValue.text = String.format("%.1f", tilt)
        binding.tvRotateValue.text = String.format("%.1f", rotate)
        
        val ip = prefs.getString("wifi_ip", DeviceRepository.wifiDeviceIp) ?: DeviceRepository.wifiDeviceIp
        binding.etWifiIp.setText(ip)
    }

    private fun setupSliders() {
        binding.sliderTilt.addOnChangeListener { _, value, _ ->
            binding.tvTiltValue.text = String.format("%.1f", value)
            saveFloat("tilt_threshold", value)
        }

        binding.sliderRotate.addOnChangeListener { _, value, _ ->
            binding.tvRotateValue.text = String.format("%.1f", value)
            saveFloat("rotate_threshold", value)
        }

        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences("gesture_settings", Context.MODE_PRIVATE)
                .edit().putBoolean("vibration_enabled", isChecked).apply()
        }
    }

    private fun setupButtons() {
        binding.btnReset.setOnClickListener {
            binding.sliderTilt.value = 5f
            binding.sliderRotate.value = 2.5f
            binding.switchVibration.isChecked = true

            getSharedPreferences("gesture_settings", Context.MODE_PRIVATE)
                .edit()
                .putFloat("tilt_threshold", 5f)
                .putFloat("rotate_threshold", 2.5f)
                .putBoolean("vibration_enabled", true)
                .apply()

            Toast.makeText(this, "Đã khôi phục cài đặt mặc định", Toast.LENGTH_SHORT).show()
        }

        binding.btnSaveIp.setOnClickListener {
            val newIp = binding.etWifiIp.text.toString().trim()
            if (newIp.isNotEmpty()) {
                getSharedPreferences("gesture_settings", Context.MODE_PRIVATE)
                    .edit()
                    .putString("wifi_ip", newIp)
                    .apply()
                DeviceRepository.wifiDeviceIp = newIp
                Toast.makeText(this, "Đã lưu IP: $newIp", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveFloat(key: String, value: Float) {
        getSharedPreferences("gesture_settings", Context.MODE_PRIVATE)
            .edit().putFloat(key, value).apply()
    }
}
