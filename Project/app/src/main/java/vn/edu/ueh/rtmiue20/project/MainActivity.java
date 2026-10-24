package vn.edu.ueh.rtmiue20.project;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;

import vn.edu.ueh.rtmiue20.project.data.model.User;
import vn.edu.ueh.rtmiue20.project.data.repository.AuthRepository;
import vn.edu.ueh.rtmiue20.project.data.repository.UserRepository;
import vn.edu.ueh.rtmiue20.project.ui.admin.AdminDashboardActivity;
import vn.edu.ueh.rtmiue20.project.ui.auth.LogInActivity;
import vn.edu.ueh.rtmiue20.project.ui.home.HomeActivity;

public class MainActivity extends AppCompatActivity {

    // Set IS_DEV_MODE to false to use Firebase Auth & Role-based navigation
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

        AuthRepository authRepository = AuthRepository.getInstance();
        android.content.SharedPreferences prefs = getSharedPreferences("BookSharePrefs", MODE_PRIVATE);
        boolean rememberMe = prefs.getBoolean("remember_me", false);

        if (authRepository.isLoggedIn() && rememberMe) {
            FirebaseUser firebaseUser = authRepository.getCurrentUser();
            if (firebaseUser != null) {
                UserRepository.getInstance().getUserProfile(firebaseUser.getUid(), new UserRepository.UserCallback() {
                    @Override
                    public void onSuccess(User userProfile) {
                        Intent intent;
                        if (userProfile != null && "admin".equalsIgnoreCase(userProfile.getRole())) {
                            intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
                        } else {
                            intent = new Intent(MainActivity.this, HomeActivity.class);
                        }
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        startActivity(new Intent(MainActivity.this, HomeActivity.class));
                        finish();
                    }
                });
            } else {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            }
        } else {
            authRepository.logout();
            startActivity(new Intent(this, LogInActivity.class));
            finish();
        }
    }
}
