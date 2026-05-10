package com.example.smartgesturecontrol.model

/**
 * Các loại cử chỉ được nhận diện bởi cảm biến
 */
enum class GestureType(val displayName: String, val emoji: String) {
    SHAKE("Vẫy tay", "👋"),
    TILT_UP("Nghiêng lên", "⬆️"),
    TILT_DOWN("Nghiêng xuống", "⬇️"),
    ROTATE_LEFT("Xoay trái", "↩️"),
    ROTATE_RIGHT("Xoay phải", "↪️"),
    FLIP("Lật điện thoại", "🔄")
}
