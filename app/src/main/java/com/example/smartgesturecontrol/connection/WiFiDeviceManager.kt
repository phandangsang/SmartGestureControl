package com.example.smartgesturecontrol.connection

import android.os.Handler
import android.os.Looper
import com.example.smartgesturecontrol.model.DeviceCommand
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Quản lý kết nối WiFi để điều khiển thiết bị IoT qua HTTP REST API
 * Gửi lệnh đến thiết bị trên mạng LAN (VD: ESP32 web server)
 *
 * Format API: POST http://<device_ip>/api/control
 * Body: {"device": "light_01", "action": "TOGGLE", "value": 0}
 */
class WiFiDeviceManager {

    var onCommandSent: ((Boolean, String) -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Gửi lệnh điều khiển qua HTTP POST
     * @param ipAddress Địa chỉ IP của thiết bị IoT trên mạng LAN
     * @param command Lệnh điều khiển cần gửi
     */
    fun sendCommand(ipAddress: String, command: DeviceCommand) {
        Thread {
            try {
                val url = URL("http://$ipAddress/api/control")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 5000
                    readTimeout = 5000
                }

                // Tạo JSON body
                val json = JSONObject().apply {
                    put("device", command.deviceId)
                    put("action", command.action)
                    put("value", command.value ?: 0)
                    put("timestamp", command.timestamp)
                }

                // Gửi request
                connection.outputStream.use { os ->
                    os.write(json.toString().toByteArray())
                    os.flush()
                }

                val responseCode = connection.responseCode
                connection.disconnect()

                mainHandler.post {
                    onCommandSent?.invoke(
                        responseCode == 200,
                        if (responseCode == 200) "Gửi lệnh thành công" else "Lỗi: HTTP $responseCode"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mainHandler.post {
                    onCommandSent?.invoke(false, "Lỗi kết nối: ${e.message}")
                }
            }
        }.start()
    }

    /**
     * Kiểm tra thiết bị có hoạt động không (ping)
     */
    fun pingDevice(ipAddress: String, callback: (Boolean) -> Unit) {
        Thread {
            try {
                val url = URL("http://$ipAddress/api/status")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                val reachable = connection.responseCode == 200
                connection.disconnect()
                mainHandler.post { callback(reachable) }
            } catch (e: Exception) {
                mainHandler.post { callback(false) }
            }
        }.start()
    }
}
