package vn.edu.ueh.rtmiue20.project.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.User;
import vn.edu.ueh.rtmiue20.project.data.repository.AuthRepository;
import vn.edu.ueh.rtmiue20.project.data.repository.UserRepository;
import vn.edu.ueh.rtmiue20.project.ui.auth.LogInActivity;
import vn.edu.ueh.rtmiue20.project.util.ViewPressEffectHelper;

public class AdminProfileActivity extends AppCompatActivity {

    private ImageView btnBack, ivAdminAvatar, btnCameraAvatar, ivHeaderAvatarIcon;
    private LinearLayout btnHeaderSave, btnSaveProfile, btnLogoutShift;
    private TextView btnChangeAvatar, btnDeleteAvatar, btnConfig2FA, btnChangePassword, btnViewFullAuditLog;
    private TextView tvAdminName, tvAdminSubtitle;

    private EditText etAdminFullName, etAdminRole, etAdminEmail, etAdminPhone;
    private Spinner spShift, spLockTimeout;
    private SwitchMaterial switchAIFlag, switchSpamAlert, switchShiftReport, switchFirestoreError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainAdminProfile), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupSpinners();
        loadAdminProfile();
        setupListeners();
        setupPressEffects();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnHeaderSave = findViewById(R.id.btnHeaderSave);
        ivHeaderAvatarIcon = findViewById(R.id.ivHeaderAvatarIcon);

        ivAdminAvatar = findViewById(R.id.ivAdminAvatar);
        btnCameraAvatar = findViewById(R.id.btnCameraAvatar);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        btnDeleteAvatar = findViewById(R.id.btnDeleteAvatar);

        tvAdminName = findViewById(R.id.tvAdminName);
        tvAdminSubtitle = findViewById(R.id.tvAdminSubtitle);

        etAdminFullName = findViewById(R.id.etAdminFullName);
        etAdminRole = findViewById(R.id.etAdminRole);
        etAdminEmail = findViewById(R.id.etAdminEmail);
        etAdminPhone = findViewById(R.id.etAdminPhone);

        spShift = findViewById(R.id.spShift);
        spLockTimeout = findViewById(R.id.spLockTimeout);

        btnConfig2FA = findViewById(R.id.btnConfig2FA);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        switchAIFlag = findViewById(R.id.switchAIFlag);
        switchSpamAlert = findViewById(R.id.switchSpamAlert);
        switchShiftReport = findViewById(R.id.switchShiftReport);
        switchFirestoreError = findViewById(R.id.switchFirestoreError);

        btnViewFullAuditLog = findViewById(R.id.btnViewFullAuditLog);

        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnLogoutShift = findViewById(R.id.btnLogoutShift);
    }

    private void setupSpinners() {
        // Shift Spinner
        String[] shifts = {
                "Ca sáng & Chiều (08:00 - 18:00) • Hà Nội",
                "Ca tối & Đêm (18:00 - 02:00) • TP. Hồ Chí Minh",
                "Trực phản ứng nhanh cuối tuần (Toàn quốc)"
        };
        ArrayAdapter<String> shiftAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, shifts);
        shiftAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spShift.setAdapter(shiftAdapter);

        // Lock Timeout Spinner
        String[] timeouts = {
                "15 phút không hoạt động (Khuyến nghị)",
                "30 phút không hoạt động",
                "60 phút không hoạt động"
        };
        ArrayAdapter<String> timeoutAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, timeouts);
        timeoutAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLockTimeout.setAdapter(timeoutAdapter);
    }

    private void loadAdminProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            UserRepository.getInstance().getUserProfile(uid, new UserRepository.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    if (user != null) {
                        String name = user.getFullName() != null && !user.getFullName().isEmpty()
                                ? user.getFullName()
                                : "Nguyễn Hoàng Nam";
                        tvAdminName.setText(name);
                        etAdminFullName.setText(name);

                        String email = user.getEmail() != null ? user.getEmail() : currentUser.getEmail();
                        if (email != null) etAdminEmail.setText(email);

                        if (user.getPhoneNumber() != null) etAdminPhone.setText(user.getPhoneNumber());

                        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                            Glide.with(AdminProfileActivity.this)
                                    .load(user.getAvatarUrl())
                                    .placeholder(R.drawable.ic_person)
                                    .circleCrop()
                                    .into(ivAdminAvatar);

                            Glide.with(AdminProfileActivity.this)
                                    .load(user.getAvatarUrl())
                                    .placeholder(R.drawable.ic_person)
                                    .circleCrop()
                                    .into(ivHeaderAvatarIcon);
                        }
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    // Fallback to default
                }
            });
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        View.OnClickListener saveAction = v -> saveAdminProfile();
        btnHeaderSave.setOnClickListener(saveAction);
        btnSaveProfile.setOnClickListener(saveAction);

        btnChangeAvatar.setOnClickListener(v -> Toast.makeText(this, "Chọn ảnh mới từ thư viện để làm avatar quản trị", Toast.LENGTH_SHORT).show());
        btnCameraAvatar.setOnClickListener(v -> Toast.makeText(this, "Mở máy ảnh chụp ảnh chân dung quản trị viên", Toast.LENGTH_SHORT).show());
        btnDeleteAvatar.setOnClickListener(v -> {
            ivAdminAvatar.setImageResource(R.drawable.ic_person);
            ivHeaderAvatarIcon.setImageResource(R.drawable.ic_person);
            Toast.makeText(this, "Đã gỡ ảnh đại diện hiện tại", Toast.LENGTH_SHORT).show();
        });

        btnConfig2FA.setOnClickListener(v -> Toast.makeText(this, "Mở giao diện cấu hình Google Authenticator / 2FA", Toast.LENGTH_SHORT).show());
        btnChangePassword.setOnClickListener(v -> Toast.makeText(this, "Gửi email khôi phục / Đổi mật khẩu quản trị", Toast.LENGTH_SHORT).show());
        btnViewFullAuditLog.setOnClickListener(v -> Toast.makeText(this, "Mở toàn bộ Nhật ký an ninh ISO/IEC 27001 (audit_logs)", Toast.LENGTH_SHORT).show());

        btnLogoutShift.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Kết thúc ca trực Admin")
                    .setMessage("Bạn có chắc chắn muốn kết thúc ca trực và đăng xuất khỏi phiên Quản trị viên?")
                    .setPositiveButton("Đăng xuất", (dialog, which) -> {
                        AuthRepository.getInstance().logout();
                        Toast.makeText(AdminProfileActivity.this, "Đã đăng xuất khỏi phiên Quản trị", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(AdminProfileActivity.this, LogInActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    private void saveAdminProfile() {
        String fullName = etAdminFullName.getText().toString().trim();
        String phone = etAdminPhone.getText().toString().trim();

        if (fullName.isEmpty()) {
            etAdminFullName.setError("Vui lòng nhập họ và tên");
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            UserRepository.getInstance().updateUserProfile(currentUser.getUid(), fullName, phone, "Admin Profile", new UserRepository.UpdateCallback() {
                @Override
                public void onSuccess() {
                    tvAdminName.setText(fullName);
                    Toast.makeText(AdminProfileActivity.this, "✓ Đã lưu thay đổi hồ sơ quản trị viên thành công!", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(AdminProfileActivity.this, "Lỗi khi lưu: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            tvAdminName.setText(fullName);
            Toast.makeText(this, "✓ Đã lưu thay đổi hồ sơ quản trị viên!", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupPressEffects() {
        ViewPressEffectHelper.applyPressEffect(
                btnBack, btnHeaderSave, btnChangeAvatar, btnDeleteAvatar,
                btnCameraAvatar, btnConfig2FA, btnChangePassword,
                btnViewFullAuditLog, btnSaveProfile, btnLogoutShift
        );
    }
}
