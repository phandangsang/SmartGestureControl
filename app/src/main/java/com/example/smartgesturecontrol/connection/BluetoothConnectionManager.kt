package com.example.smartgesturecontrol.connection

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import com.example.smartgesturecontrol.model.DeviceCommand
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Quản lý kết nối Bluetooth Classic (SPP Profile)
 * Sử dụng để giao tiếp với các thiết bị IoT như ESP32, Arduino, HC-05
 */
class BluetoothConnectionManager {

    // UUID chuẩn cho Serial Port Profile (SPP)
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var listeningThread: Thread? = null

    // Callbacks
    var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    var onMessageReceived: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    // Trạng thái
    val isBluetoothAvailable: Boolean get() = bluetoothAdapter != null
    val isBluetoothEnabled: Boolean get() = bluetoothAdapter?.isEnabled == true
    val isConnected: Boolean get() = bluetoothSocket?.isConnected == true

    /**
     * Lấy danh sách thiết bị đã ghép nối
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): Set<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices ?: emptySet()
        } catch (e: SecurityException) {
            emptySet()
        }
    }

    /**
     * Kết nối đến thiết bị Bluetooth
     * Chạy trên background thread để không block UI
     */
    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        Thread {
            try {
                // Đóng kết nối cũ nếu có
                disconnect()

                // Tạo socket và kết nối
                bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                bluetoothSocket?.connect()

                outputStream = bluetoothSocket?.outputStream
                inputStream = bluetoothSocket?.inputStream

                mainHandler.post {
                    onConnectionStateChanged?.invoke(true)
                }

                // Bắt đầu lắng nghe dữ liệu phản hồi
                startListening()

            } catch (e: IOException) {
                e.printStackTrace()
                mainHandler.post {
                    onConnectionStateChanged?.invoke(false)
                    onError?.invoke("Không thể kết nối: ${e.message}")
                }
            }
        }.start()
    }

    /**
     * Gửi lệnh điều khiển đến thiết bị IoT
     * Format: CMD:DEVICE_ID:ACTION:VALUE\n
     */
    fun sendCommand(command: DeviceCommand) {
        Thread {
            try {
                if (bluetoothSocket?.isConnected == true) {
                    val cmdString = command.toCommandString()
                    outputStream?.write(cmdString.toByteArray())
                    outputStream?.flush()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                mainHandler.post {
                    onError?.invoke("Lỗi gửi lệnh: ${e.message}")
                }
            }
        }.start()
    }

    /**
     * Lắng nghe dữ liệu phản hồi từ thiết bị
     */
    private fun startListening() {
        listeningThread = Thread {
            val buffer = ByteArray(1024)
            while (bluetoothSocket?.isConnected == true) {
                try {
                    val bytesRead = inputStream?.read(buffer) ?: -1
                    if (bytesRead > 0) {
                        val message = String(buffer, 0, bytesRead)
                        mainHandler.post {
                            onMessageReceived?.invoke(message)
                        }
                    }
                } catch (e: IOException) {
                    break
                }
            }
        }
        listeningThread?.start()
    }

    /**
     * Ngắt kết nối Bluetooth
     */
    fun disconnect() {
        try {
            listeningThread?.interrupt()
            outputStream?.close()
            inputStream?.close()
            bluetoothSocket?.close()
            mainHandler.post {
                onConnectionStateChanged?.invoke(false)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            outputStream = null
            inputStream = null
            bluetoothSocket = null
        }
    }
}
