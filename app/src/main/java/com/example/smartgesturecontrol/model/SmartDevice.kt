package com.example.smartgesturecontrol.model

/**
 * Loại thiết bị thông minh
 */
enum class DeviceType(val displayName: String, val iconRes: String) {
    LIGHT("Đèn thông minh", "ic_lightbulb"),
    FAN("Quạt", "ic_fan"),
    TV("TV", "ic_tv"),
    SPEAKER("Loa", "ic_speaker")
}

/**
 * Loại kết nối
 */
enum class ConnectionType(val displayName: String) {
    BLUETOOTH("Bluetooth"),
    WIFI("WiFi"),
    SIMULATED("Mô phỏng")
}

/**
 * Model thiết bị thông minh
 */
data class SmartDevice(
    val id: String,
    val name: String,
    val type: DeviceType,
    var isOn: Boolean = false,
    var value: Int = 50,
    val connectionType: ConnectionType = ConnectionType.SIMULATED,
    var assignedGesture: GestureType? = null
)
