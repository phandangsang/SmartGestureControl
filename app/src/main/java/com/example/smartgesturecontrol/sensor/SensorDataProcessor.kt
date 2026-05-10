package com.example.smartgesturecontrol.sensor

/**
 * Bộ xử lý dữ liệu cảm biến
 * - Low-pass filter cho Accelerometer để giảm nhiễu
 * - Moving average cho Gyroscope để ổn định dữ liệu
 */
class SensorDataProcessor {

    // Low-pass filter coefficient (0-1, cao hơn = mượt hơn nhưng chậm hơn)
    private val alpha = 0.8f
    private var filteredAccel = FloatArray(3)
    private var isFirstAccelReading = true

    // Gyroscope moving average
    private val gyroWindowSize = 5
    private val gyroHistory = ArrayDeque<FloatArray>()

    /**
     * Áp dụng Low-Pass Filter cho dữ liệu Accelerometer
     * Công thức: output[i] = alpha * output[i-1] + (1 - alpha) * input[i]
     */
    fun processAccelerometer(values: FloatArray): FloatArray {
        if (isFirstAccelReading) {
            filteredAccel = values.clone()
            isFirstAccelReading = false
            return filteredAccel.clone()
        }

        for (i in 0..2) {
            filteredAccel[i] = alpha * filteredAccel[i] + (1 - alpha) * values[i]
        }
        return filteredAccel.clone()
    }

    /**
     * Áp dụng Moving Average cho dữ liệu Gyroscope
     * Lấy trung bình của N mẫu gần nhất
     */
    fun processGyroscope(values: FloatArray): FloatArray {
        gyroHistory.addLast(values.clone())
        if (gyroHistory.size > gyroWindowSize) {
            gyroHistory.removeFirst()
        }

        val avg = FloatArray(3)
        for (sample in gyroHistory) {
            for (i in 0..2) avg[i] += sample[i]
        }
        val count = gyroHistory.size.toFloat()
        for (i in 0..2) avg[i] /= count
        return avg
    }

    /**
     * Reset tất cả bộ lọc về trạng thái ban đầu
     */
    fun reset() {
        filteredAccel = FloatArray(3)
        isFirstAccelReading = true
        gyroHistory.clear()
    }
}
