package com.example.smartgesturecontrol

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartgesturecontrol.connection.BluetoothConnectionManager
import com.example.smartgesturecontrol.connection.WiFiDeviceManager
import com.example.smartgesturecontrol.model.DeviceRepository
import com.example.smartgesturecontrol.databinding.ActivityBluetoothBinding
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BluetoothActivity - Quản lý kết nối Bluetooth với thiết bị IoT
 * Hiển thị thiết bị đã ghép nối, kết nối/ngắt kết nối
 */
class BluetoothActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBluetoothBinding
    private val btManager = BluetoothConnectionManager()
    private val logBuilder = StringBuilder()
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBluetoothBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        // Gán BluetoothManager vào DeviceRepository để cử chỉ gửi lệnh qua BT
        DeviceRepository.bluetoothManager = btManager
        // Khởi tạo WiFiManager
        DeviceRepository.wifiManager = WiFiDeviceManager()

        checkPermissions()
        setupBluetooth()
        setupUI()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            val needed = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun setupBluetooth() {
        btManager.onConnectionStateChanged = { connected ->
            runOnUiThread {
                if (connected) {
                    appendLog("✅ Đã kết nối thành công")
                    appendLog("📡 Lệnh cử chỉ sẽ được gửi qua Bluetooth")
                    binding.tvBtStatus.text = "Đã kết nối"
                    binding.tvBtStatus.setTextColor(getColor(R.color.status_on))
                    // Gán manager để DeviceRepository gửi lệnh qua BT
                    DeviceRepository.bluetoothManager = btManager
                } else {
                    appendLog("❌ Đã ngắt kết nối")
                    binding.tvBtStatus.text = "Chưa kết nối"
                    binding.tvBtStatus.setTextColor(getColor(R.color.status_off))
                }
            }
        }

        btManager.onError = { error ->
            runOnUiThread {
                appendLog("⚠️ $error")
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        }

        btManager.onMessageReceived = { message ->
            runOnUiThread {
                appendLog("📩 Nhận: $message")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupUI() {
        // Check Bluetooth availability
        if (!btManager.isBluetoothAvailable) {
            binding.tvBtStatus.text = "Không hỗ trợ Bluetooth"
            binding.tvBtStatus.setTextColor(getColor(R.color.status_off))
            binding.btnScan.isEnabled = false
            appendLog("⚠️ Thiết bị không hỗ trợ Bluetooth")
            return
        }

        if (!btManager.isBluetoothEnabled) {
            binding.tvBtStatus.text = "Bluetooth đang tắt"
            binding.tvBtStatus.setTextColor(getColor(R.color.status_warning))
            appendLog("⚠️ Vui lòng bật Bluetooth trong cài đặt")
        } else {
            binding.tvBtStatus.text = "Sẵn sàng"
            binding.tvBtStatus.setTextColor(getColor(R.color.status_on))
            appendLog("✅ Bluetooth đã sẵn sàng")
        }

        binding.btnScan.setOnClickListener {
            loadPairedDevices()
        }

        loadPairedDevices()
    }

    @SuppressLint("MissingPermission")
    private fun loadPairedDevices() {
        try {
            val pairedDevices = btManager.getPairedDevices()
            if (pairedDevices.isEmpty()) {
                binding.tvNoPaired.visibility = View.VISIBLE
                binding.recyclerPaired.visibility = View.GONE
                appendLog("ℹ️ Không tìm thấy thiết bị đã ghép nối")
            } else {
                binding.tvNoPaired.visibility = View.GONE
                binding.recyclerPaired.visibility = View.VISIBLE
                binding.recyclerPaired.layoutManager = LinearLayoutManager(this)
                binding.recyclerPaired.adapter = PairedDeviceAdapter(pairedDevices.toList())
                appendLog("🔍 Tìm thấy ${pairedDevices.size} thiết bị đã ghép nối")
            }
        } catch (e: SecurityException) {
            appendLog("⚠️ Cần cấp quyền Bluetooth")
            Toast.makeText(this, "Cần cấp quyền Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    private fun appendLog(message: String) {
        val time = dateFormat.format(Date())
        logBuilder.appendLine("[$time] $message")
        binding.tvConnectionLog.text = logBuilder.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        btManager.disconnect()
    }

    /**
     * Adapter hiển thị thiết bị Bluetooth đã ghép nối
     */
    @SuppressLint("MissingPermission")
    inner class PairedDeviceAdapter(
        private val devices: List<BluetoothDevice>
    ) : RecyclerView.Adapter<PairedDeviceAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvDeviceName)
            val tvAddress: TextView = view.findViewById(R.id.tvDeviceAddress)
            val btnConnect: MaterialButton = view.findViewById(R.id.btnConnect)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_bluetooth_device, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val device = devices[position]
            holder.tvName.text = device.name ?: "Không rõ tên"
            holder.tvAddress.text = device.address

            holder.btnConnect.setOnClickListener {
                appendLog("🔗 Đang kết nối đến ${device.name}...")
                btManager.connect(device)
            }
        }

        override fun getItemCount() = devices.size
    }
}
