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

    // Nâng cấp lên chuẩn FCM HTTP v1 API (thay thế cho API /fcm/send đã bị Google sunset)
    private static final String BASE_URL = "https://fcm.googleapis.com/v1/projects/bookshare-2ccca/messages:send";

    // FCM HTTP v1 yêu cầu OAuth2 Bearer token (được cấp bởi Firebase Admin SDK / Cloud Functions hoặc Service Account).
    // Nếu chưa cấu hình Token, ứng dụng sẽ ưu tiên sử dụng thông báo nội bộ / Badge in-app trên HomeActivity.
    private static final String OAUTH_TOKEN = "";

    public static void pushNotification(String targetToken, String title, String body) {
        if (targetToken == null || targetToken.trim().isEmpty()) {
            return;
        }

        // Nếu chưa có OAuth token, bỏ qua client-side push và dùng in-app notifications / Cloud Functions trigger
        if (OAUTH_TOKEN.isEmpty()) {
            Log.d("FCM", "FCM v1 push skipped: OAuth2 Access Token not configured (Using Cloud Functions & in-app notifications).");
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
                    Log.e("FCM", "Lỗi gửi thông báo FCM v1: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.d("FCM", "Gửi thông báo FCM v1 thành công: " + (response.body() != null ? response.body().string() : ""));
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
