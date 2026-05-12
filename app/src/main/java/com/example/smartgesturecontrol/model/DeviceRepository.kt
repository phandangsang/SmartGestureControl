package com.example.smartgesturecontrol.model

import com.example.smartgesturecontrol.connection.BluetoothConnectionManager
import com.example.smartgesturecontrol.connection.WiFiDeviceManager

/**
 * Singleton quản lý danh sách thiết bị và trạng thái
 *
 * Luồng hoạt động:
 *   Cử chỉ → GestureDetector → DeviceRepository.executeGestureAction()
 *     → 1. Cập nhật trạng thái UI (mô phỏng)
 *     → 2. Gửi lệnh qua Bluetooth/WiFi đến thiết bị IoT thật (nếu đã kết nối)
 */
object DeviceRepository {

    private val devices = mutableListOf<SmartDevice>()
    private val commandLog = mutableListOf<DeviceCommand>()
    private val listeners = mutableListOf<OnDeviceChangedListener>()

    // === Kết nối IoT ===
    // BluetoothConnectionManager dùng chung - kết nối từ BluetoothActivity
    var bluetoothManager: BluetoothConnectionManager? = null
    // WiFiDeviceManager gửi HTTP request đến IP thiết bị
    var wifiManager: WiFiDeviceManager? = null
    // IP mặc định của thiết bị WiFi IoT (VD: ESP32 web server)
    var wifiDeviceIp: String = "172.20.10.5"

    interface OnDeviceChangedListener {
        fun onDeviceStateChanged(device: SmartDevice)
        fun onCommandExecuted(command: DeviceCommand)
    }

    fun addListener(listener: OnDeviceChangedListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: OnDeviceChangedListener) {
        listeners.remove(listener)
    }

    init {
        // Khởi tạo thiết bị mặc định
        // ConnectionType cho biết lệnh sẽ được gửi qua kênh nào
        devices.addAll(listOf(
            SmartDevice("light_01", "Đèn phòng khách", DeviceType.LIGHT, false, 80,
                ConnectionType.BLUETOOTH, GestureType.SHAKE),
            SmartDevice("fan_01", "Quạt trần", DeviceType.FAN, false, 50,
                ConnectionType.BLUETOOTH, GestureType.FLIP),
            SmartDevice("tv_01", "TV Samsung", DeviceType.TV, false, 60,
                ConnectionType.WIFI, GestureType.ROTATE_LEFT),
            SmartDevice("speaker_01", "Loa thông minh", DeviceType.SPEAKER, false, 40,
                ConnectionType.WIFI, GestureType.TILT_UP)
        ))
    }

    fun getDevices(): List<SmartDevice> = devices.toList()

    fun getDeviceById(id: String): SmartDevice? = devices.find { it.id == id }

    /**
     * Bật/tắt thiết bị
     * 1. Cập nhật trạng thái trong app (UI)
     * 2. Gửi lệnh TOGGLE qua Bluetooth/WiFi đến thiết bị thật
     */
    fun toggleDevice(deviceId: String): SmartDevice? {
        val device = getDeviceById(deviceId) ?: return null
        device.isOn = !device.isOn
        val command = DeviceCommand(deviceId, "TOGGLE")
        commandLog.add(command)

        // Gửi lệnh đến thiết bị IoT thật
        sendCommandToDevice(device, command)

        listeners.forEach {
            it.onDeviceStateChanged(device)
            it.onCommandExecuted(command)
        }
        return device
    }

    /**
     * Đặt giá trị cho thiết bị (âm lượng, độ sáng, tốc độ)
     * 1. Cập nhật giá trị trong app (UI)
     * 2. Gửi lệnh SET_VALUE qua Bluetooth/WiFi
     */
    fun setDeviceValue(deviceId: String, value: Int): SmartDevice? {
        val device = getDeviceById(deviceId) ?: return null
        device.value = value.coerceIn(0, 100)
        val command = DeviceCommand(deviceId, "SET_VALUE", value)
        commandLog.add(command)

        // Gửi lệnh đến thiết bị IoT thật
        sendCommandToDevice(device, command)

        listeners.forEach {
            it.onDeviceStateChanged(device)
            it.onCommandExecuted(command)
        }
        return device
    }

    /**
     * Gửi lệnh đến thiết bị IoT thật qua Bluetooth hoặc WiFi
     *
     * Bluetooth: Gửi chuỗi "CMD:light_01:TOGGLE:0\n" qua SPP socket
     *   → Thiết bị IoT (ESP32/Arduino) nhận chuỗi, parse, điều khiển relay/LED
     *
     * WiFi: Gửi HTTP POST đến http://192.168.1.100/api/control
     *   → Body JSON: {"device":"light_01","action":"TOGGLE","value":0}
     *   → ESP32 web server nhận request, điều khiển thiết bị
     */
    private fun sendCommandToDevice(device: SmartDevice, command: DeviceCommand) {
        when (device.connectionType) {
            ConnectionType.BLUETOOTH -> {
                // Gửi qua Bluetooth Classic (SPP)
                // Format: "CMD:light_01:TOGGLE:0\n"
                bluetoothManager?.let { bt ->
                    if (bt.isConnected) {
                        bt.sendCommand(command)
                    }
                }
            }
            ConnectionType.WIFI -> {
                // Gửi qua WiFi HTTP POST
                // URL: http://192.168.1.100/api/control
                // Body: {"device":"tv_01","action":"TOGGLE","value":0}
                wifiManager?.sendCommand(wifiDeviceIp, command)
            }
            ConnectionType.SIMULATED -> {
                // Chỉ mô phỏng trên UI, không gửi lệnh thật
            }
        }
    }

    /**
     * Thực hiện hành động dựa trên cử chỉ
     * Được gọi khi GestureDetector phát hiện cử chỉ
     *
     * Luồng: Cử chỉ → tìm thiết bị được gán cử chỉ đó → thực hiện hành động
     *   → cập nhật UI + gửi lệnh qua BT/WiFi
     */
    fun executeGestureAction(gesture: GestureType): DeviceCommand? {
        return when (gesture) {
            GestureType.SHAKE, GestureType.FLIP -> {
                val deviceId = if (gesture == GestureType.SHAKE) "light_01" else "fan_01"
                val device = getDeviceById(deviceId) ?: return null
                toggleDevice(device.id)
                DeviceCommand(device.id, "TOGGLE")
            }
            GestureType.TILT_UP -> {
                val device = getDeviceById("speaker_01") ?: return null
                val newVal = (device.value + 10).coerceAtMost(100)
                device.value = newVal
                DeviceCommand(device.id, "VOLUME_UP", newVal).also {
                    commandLog.add(it)
                    sendCommandToDevice(device, it)
                    listeners.forEach { l -> l.onDeviceStateChanged(device); l.onCommandExecuted(it) }
                }
            }
            GestureType.TILT_DOWN -> {
                val device = getDeviceById("speaker_01") ?: return null
                val newVal = (device.value - 10).coerceAtLeast(0)
                device.value = newVal
                DeviceCommand(device.id, "VOLUME_DOWN", newVal).also {
                    commandLog.add(it)
                    sendCommandToDevice(device, it)
                    listeners.forEach { l -> l.onDeviceStateChanged(device); l.onCommandExecuted(it) }
                }
            }
            GestureType.ROTATE_LEFT -> {
                val device = getDeviceById("tv_01") ?: return null
                DeviceCommand(device.id, "PREVIOUS").also {
                    commandLog.add(it)
                    sendCommandToDevice(device, it)
                }
            }
            GestureType.ROTATE_RIGHT -> {
                val device = getDeviceById("tv_01") ?: return null
                DeviceCommand(device.id, "NEXT").also {
                    commandLog.add(it)
                    sendCommandToDevice(device, it)
                }
            }
        }
    }

    fun getCommandLog(): List<DeviceCommand> = commandLog.toList()

    fun addDevice(device: SmartDevice) {
        devices.add(device)
    }

    fun removeDevice(deviceId: String) {
        devices.removeAll { it.id == deviceId }
    }
}
