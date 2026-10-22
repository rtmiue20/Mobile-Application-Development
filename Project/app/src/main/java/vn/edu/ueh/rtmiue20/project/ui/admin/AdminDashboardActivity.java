package vn.edu.ueh.rtmiue20.project.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import vn.edu.ueh.rtmiue20.project.MainActivity;
import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.repository.AuthRepository;
import vn.edu.ueh.rtmiue20.project.util.ViewPressEffectHelper;

public class AdminDashboardActivity extends AppCompatActivity {

    private ImageView btnBack;
    private ImageView btnLog;
    private TextView btnCheckNow;
    private View btnLogoutAdmin;

    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_dashboard);

        View mainView = findViewById(R.id.mainAdminDashboard);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        authRepository = AuthRepository.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnLog = findViewById(R.id.btnLog);
        btnCheckNow = findViewById(R.id.btnCheckNow);
        btnLogoutAdmin = findViewById(R.id.btnLogoutAdmin);
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnLogoutAdmin != null) {
            btnLogoutAdmin.setOnClickListener(v -> handleLogout());
        }

        if (btnLog != null) {
            btnLog.setOnClickListener(v -> 
                Toast.makeText(this, "Nhật ký hệ thống Quản trị", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnCheckNow != null) {
            btnCheckNow.setOnClickListener(v -> 
                Toast.makeText(this, "Đang mở danh sách 18 tin đăng chờ duyệt...", Toast.LENGTH_SHORT).show()
            );
        }

        View cardTotalMembers = findViewById(R.id.cardTotalMembers);
        if (cardTotalMembers != null) {
            cardTotalMembers.setOnClickListener(v -> 
                startActivity(new Intent(AdminDashboardActivity.this, UserManagementActivity.class))
            );
        }

        View btnManageMembers = findViewById(R.id.btnManageMembers);
        if (btnManageMembers != null) {
            btnManageMembers.setOnClickListener(v -> 
                startActivity(new Intent(AdminDashboardActivity.this, UserManagementActivity.class))
            );
        }

        ViewPressEffectHelper.applyPressEffect(
                btnLogoutAdmin, btnBack, cardTotalMembers, btnManageMembers
        );
    }

    private void handleLogout() {
        authRepository.logout();
        Toast.makeText(this, "Đã đăng xuất khỏi Trang Quản Trị.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(AdminDashboardActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
