package com.example.smartgesturecontrol

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartgesturecontrol.adapter.DeviceAdapter
import com.example.smartgesturecontrol.databinding.ActivityDeviceListBinding
import com.example.smartgesturecontrol.model.*

/**
 * DeviceListActivity - Quản lý danh sách thiết bị IoT
 * Hiển thị, thêm, bật/tắt, điều chỉnh thiết bị
 */
class DeviceListActivity : AppCompatActivity(), DeviceRepository.OnDeviceChangedListener {

    private lateinit var binding: ActivityDeviceListBinding
    private lateinit var adapter: DeviceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        DeviceRepository.addListener(this)

        setupRecyclerView()
        setupFab()
    }

    private fun setupRecyclerView() {
        adapter = DeviceAdapter(
            DeviceRepository.getDevices().toMutableList(),
            onToggle = { device ->
                DeviceRepository.toggleDevice(device.id)
            },
            onValueChange = { device, value ->
                DeviceRepository.setDeviceValue(device.id, value)
            }
        )
        binding.recyclerDevices.layoutManager = LinearLayoutManager(this)
        binding.recyclerDevices.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddDevice.setOnClickListener {
            showAddDeviceDialog()
        }
    }

    private fun showAddDeviceDialog() {
        val deviceTypes = DeviceType.values().map { it.displayName }.toTypedArray()
        var selectedType = DeviceType.LIGHT

        AlertDialog.Builder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle("Thêm thiết bị mới")
            .setSingleChoiceItems(deviceTypes, 0) { _, which ->
                selectedType = DeviceType.values()[which]
            }
            .setPositiveButton("Thêm") { _, _ ->
                val id = "${selectedType.name.lowercase()}_${System.currentTimeMillis()}"
                val device = SmartDevice(
                    id = id,
                    name = "${selectedType.displayName} mới",
                    type = selectedType
                )
                DeviceRepository.addDevice(device)
                adapter.updateDevices(DeviceRepository.getDevices().toMutableList())
                Toast.makeText(this, "Đã thêm ${device.name}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDeviceStateChanged(device: SmartDevice) {
        runOnUiThread {
            adapter.updateDevices(DeviceRepository.getDevices().toMutableList())
        }
    }

    override fun onCommandExecuted(command: DeviceCommand) {}

    override fun onDestroy() {
        super.onDestroy()
        DeviceRepository.removeListener(this)
    }
}
