package vn.edu.ueh.rtmiue20.project.ui.base;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import vn.edu.ueh.rtmiue20.project.ui.admin.AdminDashboardActivity;
import vn.edu.ueh.rtmiue20.project.ui.home.HomeActivity;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Đăng ký sự kiện lắng nghe nút Back vật lý/cử chỉ vuốt của hệ thống
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleSmartBack();
            }
        });
    }

    /**
     * Hàm xử lý điều hướng Back thông minh dùng chung cho toàn bộ App.
     * Các Activity con có thể gọi trực tiếp hàm này cho các nút Back (Mũi tên) trên giao diện.
     */
    protected void handleSmartBack() {
        if (isTaskRoot()) {
            SharedPreferences prefs = getSharedPreferences("BookSharePrefs", MODE_PRIVATE);
            String role = prefs.getString("user_role", "");
            Class<?> targetHome = "admin".equalsIgnoreCase(role)
                    ? AdminDashboardActivity.class
                    : HomeActivity.class;
            Intent intent = new Intent(this, targetHome);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        // Đóng màn hình hiện tại
        finish();
    }
}
