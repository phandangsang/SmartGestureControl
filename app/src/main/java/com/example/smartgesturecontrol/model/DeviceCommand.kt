package com.example.smartgesturecontrol.model

/**
 * Lệnh điều khiển gửi đến thiết bị IoT
 */
data class DeviceCommand(
    val deviceId: String,
    val action: String,      // TOGGLE, SET_VALUE, NEXT, PREVIOUS
    val value: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toCommandString(): String {
        return "CMD:$deviceId:$action:${value ?: 0}\n"
    }

    override fun toString(): String {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
        return "[$time] $action → $deviceId ${if (value != null) "(value=$value)" else ""}"
    }
}
