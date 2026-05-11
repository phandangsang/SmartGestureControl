package com.example.smartgesturecontrol.hardware

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.view.KeyEvent
import android.util.Log

class PhoneHardwareController(private val context: Context) {

    private val cameraManager: CameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    
    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private var isFlashlightOn = false
    private var cameraId: String? = null

    init {
        try {
            // Lấy ID của camera mặt sau
            val idList = cameraManager.cameraIdList
            if (idList.isNotEmpty()) {
                cameraId = idList[0]
                
                // Đăng ký callback để theo dõi trạng thái đèn flash từ các app khác
                cameraManager.registerTorchCallback(object : CameraManager.TorchCallback() {
                    override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                        super.onTorchModeChanged(cameraId, enabled)
                        if (cameraId == this@PhoneHardwareController.cameraId) {
                            isFlashlightOn = enabled
                        }
                    }
                }, null)
            }
        } catch (e: CameraAccessException) {
            Log.e("HardwareController", "Không thể truy cập Camera: ${e.message}")
        }
    }

    /**
     * Bật/tắt đèn Flash
     * @return trạng thái sau khi toggle
     */
    fun toggleFlashlight(): Boolean {
        if (cameraId == null) return false
        
        try {
            val newState = !isFlashlightOn
            cameraManager.setTorchMode(cameraId!!, newState)
            isFlashlightOn = newState
            return isFlashlightOn
        } catch (e: CameraAccessException) {
            Log.e("HardwareController", "Lỗi khi bật/tắt flash: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.e("HardwareController", "Lỗi tham số flash: ${e.message}")
        }
        return isFlashlightOn
    }
    
    /**
     * Lấy trạng thái hiện tại của đèn Flash
     */
    fun getFlashlightState(): Boolean {
        return isFlashlightOn
    }

    /**
     * Tăng/giảm âm lượng Media
     * @param increase true = tăng, false = giảm
     * @return Âm lượng hiện tại sau khi chỉnh (dưới dạng %)
     */
    fun adjustVolume(increase: Boolean): Int {
        val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        // Cờ FLAG_SHOW_UI để hiện thanh âm lượng trên màn hình
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        return getVolumePercentage()
    }
    
    /**
     * Tính toán % âm lượng hiện tại
     */
    fun getVolumePercentage(): Int {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (max == 0) return 0
        return (current * 100) / max
    }

    /**
     * Phát sự kiện chuyển bài hát (Media Key)
     * @param next true = bài tiếp theo, false = bài trước
     */
    fun changeSong(next: Boolean) {
        val keyCode = if (next) KeyEvent.KEYCODE_MEDIA_NEXT else KeyEvent.KEYCODE_MEDIA_PREVIOUS
        
        // Cần giả lập cả ACTION_DOWN và ACTION_UP để hệ thống nhận được như một lần bấm nút
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        
        audioManager.dispatchMediaKeyEvent(downEvent)
        audioManager.dispatchMediaKeyEvent(upEvent)
    }

    /**
     * Phát/Tạm dừng nhạc
     */
    fun togglePlayPause() {
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        audioManager.dispatchMediaKeyEvent(downEvent)
        audioManager.dispatchMediaKeyEvent(upEvent)
    }
}
