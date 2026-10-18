package vn.edu.ueh.rtmiue20.project;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // ==========================================
    // DEV MODE: Đổi giá trị này thành true nếu muốn bypass màn hình Login, 
    // đi thẳng vào HomeActivity (Trang chủ) để thiết kế giao diện bên trong app.
    // Khi chuẩn bị phát hành hoặc đã cấu hình xong Firebase, hãy đổi lại thành false.
    // ==========================================
    private static final boolean IS_DEV_MODE = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (IS_DEV_MODE) {
            // Chế độ phát triển: Vào thẳng trang chủ
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        // Chế độ thực tế (Có Firebase)
        AuthRepository authRepository = AuthRepository.getInstance();
        if (authRepository.isLoggedIn()) {
            // Nếu đã đăng nhập, chuyển sang màn hình Trang chủ
            startActivity(new Intent(this, HomeActivity.class));
        } else {
            // Nếu chưa đăng nhập, chuyển sang màn hình Đăng nhập
            startActivity(new Intent(this, LogInActivity.class));
        }
        finish();
    }
}
