import ctypes
from flask import Flask, request, jsonify

app = Flask(__name__)

# Các mã phím (Virtual Key Codes) trên Windows để điều khiển Media
VK_VOLUME_DOWN = 0xAE
VK_VOLUME_UP = 0xAF
VK_MEDIA_NEXT_TRACK = 0xB0
VK_MEDIA_PREV_TRACK = 0xB1
VK_MEDIA_PLAY_PAUSE = 0xB3

def press_key(hexKeyCode):
    """Giả lập việc bấm một phím trên bàn phím Windows"""
    # 0 = KEYEVENTF_KEYDOWN, 2 = KEYEVENTF_KEYUP
    ctypes.windll.user32.keybd_event(hexKeyCode, 0, 0, 0)
    ctypes.windll.user32.keybd_event(hexKeyCode, 0, 2, 0)

# 1. Endpoint để App kiểm tra kết nối (Ping)
@app.route('/api/status', methods=['GET'])
def status():
    return jsonify({"status": "Laptop is ready!"}), 200

# 2. Endpoint để nhận lệnh điều khiển cử chỉ
@app.route('/api/control', methods=['POST'])
def control():
    # Phân tích dữ liệu JSON điện thoại gửi sang
    data = request.get_json()
    device = data.get("device", "Unknown")
    action = data.get("action", "Unknown")
    value = data.get("value", 0)
    
    print("\n" + "="*40)
    print(f"[🔥 NHẬN LỆNH TỪ ĐIỆN THOẠI]")
    print(f"👉 Tên thiết bị : {device}")
    print(f"👉 Hành động   : {action}")
    print(f"👉 Giá trị     : {value}")
    
    # === ĐIỀU KHIỂN LAPTOP WINDOWS ===
    if action == "VOLUME_UP":
        print("🔊 Đang TĂNG âm lượng Windows...")
        # Bấm nút tăng âm lượng 5 lần (mỗi lần tăng 2%, tổng cộng tăng 10%)
        for _ in range(5):
            press_key(VK_VOLUME_UP)
            
    elif action == "VOLUME_DOWN":
        print("🔉 Đang GIẢM âm lượng Windows...")
        for _ in range(5):
            press_key(VK_VOLUME_DOWN)
            
    elif action == "PREVIOUS":
        print("⏮ Đang lùi bài hát trên Windows...")
        press_key(VK_MEDIA_PREV_TRACK)
        
    elif action == "NEXT":
        print("⏭ Đang nhảy bài hát trên Windows...")
        press_key(VK_MEDIA_NEXT_TRACK)
        
    elif action == "TOGGLE":
        print("⏯ Play/Pause nhạc trên Windows...")
        press_key(VK_MEDIA_PLAY_PAUSE)
        
    print("="*40)
    
    return jsonify({"message": "Laptop đã nhận và thực thi lệnh"}), 200

if __name__ == '__main__':
    print("Đang khởi động Server chờ lệnh từ điện thoại...")
    print("Sẵn sàng điều khiển Nhạc và Âm lượng Windows!")
    # Chạy server ở port 80
    app.run(host='0.0.0.0', port=80)
