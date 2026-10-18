package vn.edu.ueh.rtmiue20.project;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import vn.edu.ueh.rtmiue20.project.data.repository.AuthRepository;
import vn.edu.ueh.rtmiue20.project.ui.auth.LogInActivity;
import vn.edu.ueh.rtmiue20.project.ui.home.HomeActivity;

public class MainActivity extends AppCompatActivity {

    // Set IS_DEV_MODE to false to use Firebase Auth flow
    private static final boolean IS_DEV_MODE = false;

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
