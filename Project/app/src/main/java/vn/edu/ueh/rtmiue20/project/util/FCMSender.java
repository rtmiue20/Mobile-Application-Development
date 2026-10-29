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

public class FCMSender {

    // Đường dẫn API cũ của Firebase Cloud Messaging (đã deprecated/sunset)
    private static final String BASE_URL = "https://fcm.googleapis.com/fcm/send";

    // Server Key giữ chỗ (nếu chưa cấu hình, ứng dụng sẽ ưu tiên sử dụng thông báo nội bộ / Badge in-app)
    private static final String SERVER_KEY = "key=ĐIỀN_SERVER_KEY_CỦA_FIREBASE_VÀO_ĐÂY";

    public static void pushNotification(String targetToken, String title, String body) {
        if (targetToken == null || targetToken.trim().isEmpty()) {
            return;
        }

        // Nếu chưa cấu hình Server Key thật, bỏ qua yêu cầu đẩy FCM API cũ để tránh lỗi mạng
        // và tập trung vào cơ chế thông báo nội bộ (Badge số yêu cầu ở góc chuông HomeActivity).
        if (SERVER_KEY.contains("ĐIỀN_SERVER_KEY")) {
            Log.d("FCM", "FCM push notification skipped: Server key is not configured (Using in-app badge notifications).");
            return;
        }

        OkHttpClient client = new OkHttpClient();
        JSONObject json = new JSONObject();
        try {
            JSONObject notification = new JSONObject();
            notification.put("title", title);
            notification.put("body", body);

            json.put("to", targetToken);
            json.put("notification", notification);

            RequestBody requestBody = RequestBody.create(
                    json.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL)
                    .post(requestBody)
                    .addHeader("Authorization", SERVER_KEY)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("FCM", "Lỗi gửi thông báo: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.d("FCM", "Gửi thông báo thành công: " + (response.body() != null ? response.body().string() : ""));
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
