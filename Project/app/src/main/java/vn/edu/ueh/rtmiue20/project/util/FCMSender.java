package vn.edu.ueh.rtmiue20.project.util;

import android.util.Log;

/**
 * LƯU Ý KIẾN TRÚC FIREBASE CLOUD MESSAGING (FR11):
 *
 * 1. Endpoint cũ "https://fcm.googleapis.com/fcm/send" (FCM Legacy HTTP API) đã bị Google khai tử
 *    hoàn toàn từ tháng 06/2024, đồng thời việc lưu Server Key trong client APK vi phạm bảo mật.
 *
 * 2. Toàn bộ logic gửi thông báo đẩy FCM v1 đã được chuyển đổi sang Serverless Triggers
 *    trên Firebase Cloud Functions (xem chi tiết trong file functions/index.js):
 *    - onRequestCreated: Tự động gửi thông báo FCM v1 tới chủ sách khi có người gửi yêu cầu nhận sách mới.
 *    - onRequestUpdated: Tự động gửi thông báo FCM v1 tới người xin khi chủ sách duyệt/từ chối/hoàn tất.
 *    - onCommentCreated: Tự động gửi thông báo FCM v1 tới chủ sách khi có bình luận mới.
 *
 * 3. Phía Client kết hợp với Firestore Realtime Listener trong HomeActivity (observeNotificationData)
 *    để cập nhật huy hiệu chuông thông báo tức thì theo thời gian thực.
 */
public final class FCMSender {

    private static final String TAG = "FCMSender";

    private FCMSender() {
    }

    /**
     * Phương thức tương thích ngược. Logic gửi thông báo thật sự được tự động kích hoạt
     * bởi Cloud Functions khi tài liệu Firestore tương ứng được tạo hoặc cập nhật.
     */
    public static void pushNotification(String targetToken, String title, String body) {
        Log.d(TAG, "FCM Notification delegated to Cloud Functions trigger: [" + title + "] " + body);
    }
}
