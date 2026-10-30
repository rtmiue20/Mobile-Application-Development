package vn.edu.ueh.rtmiue20.project.util;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * LƯU Ý KIẾN TRÚC FIREBASE CLOUD MESSAGING (FR11):
 *
 * 1. Nâng cấp lên chuẩn FCM HTTP v1 API ("https://fcm.googleapis.com/v1/projects/bookshare-2ccca/messages:send"),
 *    thay thế hoàn toàn cho endpoint cũ "/fcm/send" đã bị Google khai tử từ tháng 06/2024.
 *
 * 2. FCM v1 yêu cầu OAuth2 Bearer token (được cấp bởi Firebase Admin SDK / Cloud Functions hoặc Service Account).
 *    Toàn bộ logic gửi thông báo đẩy FCM v1 tự động được xử lý qua Serverless Triggers
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

    // Chuẩn FCM HTTP v1 API
    private static final String BASE_URL = "https://fcm.googleapis.com/v1/projects/bookshare-2ccca/messages:send";

    // FCM HTTP v1 yêu cầu OAuth2 Bearer token ngắn hạn (Short-lived token).
    // Nếu để trống, ứng dụng tự động ủy thác việc gửi cho Cloud Functions triggers và in-app notifications.
    private static final String OAUTH_TOKEN = "";

    private FCMSender() {
    }

    /**
     * Gửi thông báo đẩy FCM. Nếu OAUTH_TOKEN chưa được cấu hình tại Client,
     * hệ thống tự động dựa vào Cloud Functions triggers trên Firestore để gửi an toàn.
     */
    public static void pushNotification(String targetToken, String title, String body) {
        if (targetToken == null || targetToken.trim().isEmpty()) {
            return;
        }

        // Nếu chưa cấu hình OAuth2 token tại Client, ủy thác gửi cho Cloud Functions & In-App Notification
        if (OAUTH_TOKEN.isEmpty()) {
            Log.d(TAG, "FCM Notification delegated to Cloud Functions trigger & in-app notifications: [" + title + "] " + body);
            return;
        }

        OkHttpClient client = new OkHttpClient();
        JSONObject json = new JSONObject();
        try {
            JSONObject message = new JSONObject();
            message.put("token", targetToken);

            JSONObject notification = new JSONObject();
            notification.put("title", title);
            notification.put("body", body);

            message.put("notification", notification);
            json.put("message", message);

            RequestBody requestBody = RequestBody.create(
                    json.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + OAUTH_TOKEN)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Lỗi gửi thông báo FCM v1: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.d(TAG, "Gửi thông báo FCM v1 thành công: " + (response.body() != null ? response.body().string() : ""));
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Lỗi đóng gói JSON FCM v1: " + e.getMessage(), e);
        }
    }
}
