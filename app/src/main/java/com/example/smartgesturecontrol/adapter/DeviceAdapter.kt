package com.example.smartgesturecontrol.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartgesturecontrol.R
import com.example.smartgesturecontrol.model.DeviceType
import com.example.smartgesturecontrol.model.SmartDevice
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * Adapter hiển thị danh sách thiết bị IoT trong RecyclerView
 */
class DeviceAdapter(
    private val devices: MutableList<SmartDevice>,
    private val onToggle: (SmartDevice) -> Unit,
    private val onValueChange: (SmartDevice, Int) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    inner class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconView: ImageView = view.findViewById(R.id.deviceIcon)
        val nameView: TextView = view.findViewById(R.id.deviceName)
        val typeView: TextView = view.findViewById(R.id.deviceType)
        val statusView: TextView = view.findViewById(R.id.deviceStatus)
        val switchView: MaterialSwitch = view.findViewById(R.id.deviceSwitch)
        val gestureView: TextView = view.findViewById(R.id.deviceGesture)
        val valueView: TextView = view.findViewById(R.id.deviceValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]

        holder.nameView.text = device.name
        holder.typeView.text = device.type.displayName

        // Icon theo loại thiết bị
        val iconRes = when (device.type) {
            DeviceType.LIGHT -> R.drawable.ic_lightbulb
            DeviceType.FAN -> R.drawable.ic_fan
            DeviceType.TV -> R.drawable.ic_tv
            DeviceType.SPEAKER -> R.drawable.ic_speaker
        }
        holder.iconView.setImageResource(iconRes)

        // Trạng thái bật/tắt
        holder.switchView.isChecked = device.isOn
        holder.statusView.text = if (device.isOn) "BẬT" else "TẮT"
        holder.statusView.setTextColor(
            holder.itemView.context.getColor(
                if (device.isOn) R.color.status_on else R.color.status_off
            )
        )

        // Giá trị (âm lượng, độ sáng, tốc độ)
        holder.valueView.text = "${device.value}%"

        // Cử chỉ được gán
        holder.gestureView.text = device.assignedGesture?.let {
            "${it.emoji} ${it.displayName}"
        } ?: "Chưa gán cử chỉ"

        // Sự kiện toggle
        holder.switchView.setOnCheckedChangeListener { _, _ ->
            onToggle(device)
        }
    }

    override fun getItemCount(): Int = devices.size

    fun updateDevices(newDevices: List<SmartDevice>) {
        devices.clear()
        devices.addAll(newDevices)
        notifyDataSetChanged()
    }

    fun updateDevice(device: SmartDevice) {
        val index = devices.indexOfFirst { it.id == device.id }
        if (index >= 0) {
            devices[index] = device
            notifyItemChanged(index)
        }
    }
}
