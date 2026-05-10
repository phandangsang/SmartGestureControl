package com.example.smartgesturecontrol.sensor

import com.example.smartgesturecontrol.model.GestureType
import kotlin.math.abs

/**
 * Engine nhận diện cử chỉ từ dữ liệu cảm biến
 *
 * - SHAKE (Vẫy tay): Proximity Sensor - tay vẫy qua trước điện thoại (GẦN → XA)
 * - TILT: Accelerometer trục Y - nghiêng điện thoại lên/xuống
 * - ROTATE: Gyroscope trục Z - xoay điện thoại sang trái/phải
 * - FLIP: Accelerometer trục Z - lật úp/ngửa điện thoại
 */
class GestureDetector(
    private val processor: SensorDataProcessor = SensorDataProcessor()
) {

    interface OnGestureDetectedListener {
        fun onGestureDetected(gesture: GestureType)
        fun onSensorDataUpdated(accelData: FloatArray, gyroData: FloatArray)
    }

    var listener: OnGestureDetectedListener? = null

    // === Ngưỡng nhận diện ===
    var tiltThreshold = 5f        // m/s²
    var rotateThreshold = 2.5f    // rad/s
    var flipThreshold = 7f        // m/s²

    // === Debounce ===
    private var lastGestureTime = 0L
    private val debounceDuration = 1000L

    // === Accelerometer state ===
    private var lastZValue = 0f
    private var isFirstReading = true
    private var currentAccelData = FloatArray(3)
    private var currentGyroData = FloatArray(3)

    // === Proximity hand wave ===
    private var isNear = false
    private var nearTimestamp = 0L
    private val waveMaxDuration = 1500L

    // === Bật/tắt từng loại cử chỉ ===
    var waveEnabled = true
    var tiltEnabled = true
    var rotateEnabled = true
    var flipEnabled = true

    /**
     * Proximity Sensor → phát hiện vẫy tay (SHAKE)
     * Điện thoại nằm yên, tay vẫy qua → GẦN rồi XA
     */
    fun onProximityData(distance: Float, maxRange: Float) {
        val currentlyNear = distance < maxRange

        if (currentlyNear && !isNear) {
            isNear = true
            nearTimestamp = System.currentTimeMillis()
        } else if (!currentlyNear && isNear) {
            isNear = false
            val duration = System.currentTimeMillis() - nearTimestamp
            if (duration in 50..waveMaxDuration && waveEnabled) {
                emitGesture(GestureType.SHAKE)
            }
        }
    }

    /**
     * Accelerometer → phát hiện TILT và FLIP
     */
    fun onAccelerometerData(values: FloatArray) {
        val filtered = processor.processAccelerometer(values)
        currentAccelData = filtered
        listener?.onSensorDataUpdated(currentAccelData, currentGyroData)

        // TILT: nghiêng trục Y
        if (tiltEnabled) {
            if (filtered[1] > tiltThreshold) {
                emitGesture(GestureType.TILT_UP)
            } else if (filtered[1] < -tiltThreshold) {
                emitGesture(GestureType.TILT_DOWN)
            }
        }

        // FLIP: thay đổi đột ngột trục Z
        if (flipEnabled && !isFirstReading) {
            val zChange = abs(filtered[2] - lastZValue)
            if (zChange > flipThreshold) {
                emitGesture(GestureType.FLIP)
            }
        }

        lastZValue = filtered[2]
        isFirstReading = false
    }

    /**
     * Gyroscope → phát hiện ROTATE
     */
    fun onGyroscopeData(values: FloatArray) {
        val filtered = processor.processGyroscope(values)
        currentGyroData = filtered
        listener?.onSensorDataUpdated(currentAccelData, currentGyroData)

        if (rotateEnabled) {
            if (filtered[2] > rotateThreshold) {
                emitGesture(GestureType.ROTATE_LEFT)
            } else if (filtered[2] < -rotateThreshold) {
                emitGesture(GestureType.ROTATE_RIGHT)
            }
        }
    }

    private fun emitGesture(gesture: GestureType) {
        val now = System.currentTimeMillis()
        if (now - lastGestureTime > debounceDuration) {
            lastGestureTime = now
            listener?.onGestureDetected(gesture)
        }
    }

    fun reset() {
        processor.reset()
        isFirstReading = true
        lastGestureTime = 0
        isNear = false
        nearTimestamp = 0
        currentAccelData = FloatArray(3)
        currentGyroData = FloatArray(3)
    }
}
