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

    // Đường dẫn API của Firebase Cloud Messaging
    private static final String BASE_URL = "https://fcm.googleapis.com/fcm/send";

    // TODO: Leader cần cung cấp SERVER KEY từ Firebase Console để điền vào đây
    private static final String SERVER_KEY = "key=ĐIỀN_SERVER_KEY_CỦA_FIREBASE_VÀO_ĐÂY";

    public static void pushNotification(String targetToken, String title, String body) {
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
                    Log.d("FCM", "Gửi thông báo thành công: " + response.body().string());
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}