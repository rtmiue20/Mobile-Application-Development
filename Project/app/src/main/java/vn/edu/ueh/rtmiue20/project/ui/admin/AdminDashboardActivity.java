package vn.edu.ueh.rtmiue20.project.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.repository.AuthRepository;
import vn.edu.ueh.rtmiue20.project.ui.auth.LogInActivity;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        View btnLogout = findViewById(R.id.btnLogoutAdmin);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                AuthRepository.getInstance().logout();
                Toast.makeText(this, "Đã đăng xuất khỏi Admin", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(AdminDashboardActivity.this, LogInActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View cardTotalMembers = findViewById(R.id.cardTotalMembers);
        if (cardTotalMembers != null) {
            cardTotalMembers.setOnClickListener(v -> {
                startActivity(new Intent(AdminDashboardActivity.this, UserManagementActivity.class));
            });
        }

        View btnManageMembers = findViewById(R.id.btnManageMembers);
        if (btnManageMembers != null) {
            btnManageMembers.setOnClickListener(v -> {
                startActivity(new Intent(AdminDashboardActivity.this, UserManagementActivity.class));
            });
        }

        vn.edu.ueh.rtmiue20.project.util.ViewPressEffectHelper.applyPressEffect(
                btnLogout, btnBack, cardTotalMembers, btnManageMembers
        );
    }
}
