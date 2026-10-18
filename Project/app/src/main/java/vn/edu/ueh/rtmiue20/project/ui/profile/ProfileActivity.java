package vn.edu.ueh.rtmiue20.project.ui.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseUser;

import vn.edu.ueh.rtmiue20.project.MainActivity;
import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.User;
import vn.edu.ueh.rtmiue20.project.data.repository.AuthRepository;
import vn.edu.ueh.rtmiue20.project.data.repository.UserRepository;

public class ProfileActivity extends AppCompatActivity {

    private ImageView ivAvatar;
    private TextView tvProfileFullName;
    private TextView tvProfileEmail;
    private TextView tvProfileRole;
    private View btnEditProfile;
    private View btnLogoutProfile;

    private AuthRepository authRepository;
    private UserRepository userRepository;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainProfile), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        authRepository = AuthRepository.getInstance();
        userRepository = UserRepository.getInstance();

        initViews();
        setupImagePickerLauncher();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.ivAvatar);
        tvProfileFullName = findViewById(R.id.tvProfileFullName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileRole = findViewById(R.id.tvProfileRole);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogoutProfile = findViewById(R.id.btnLogoutProfile);
    }

    private void setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        Uri imageUri = result.getData().getData();
                        uploadAvatarImage(imageUri);
                    }
                }
        );
    }

    private void setupListeners() {
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
                startActivity(intent);
            });
        }

        if (ivAvatar != null) {
            ivAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                imagePickerLauncher.launch(intent);
            });
        }

        if (btnLogoutProfile != null) {
            btnLogoutProfile.setOnClickListener(v -> handleLogout());
        }
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Phiên đăng nhập đã hết hạn.", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        if (tvProfileEmail != null && currentUser.getEmail() != null) {
            tvProfileEmail.setText(currentUser.getEmail());
        }

        userRepository.getUserProfile(currentUser.getUid(), new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (tvProfileFullName != null) {
                    tvProfileFullName.setText(user.getFullName() != null && !user.getFullName().isEmpty() ? user.getFullName() : "Nguyễn Hà Linh");
                }

                if (tvProfileRole != null) {
                    if ("admin".equalsIgnoreCase(user.getRole())) {
                        tvProfileRole.setText("✔ Quản trị viên (Admin)");
                    } else {
                        tvProfileRole.setText("✔ Thành viên uy tín 5 sao");
                    }
                }

                if (ivAvatar != null) {
                    if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                        Glide.with(ProfileActivity.this)
                                .load(user.getAvatarUrl())
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .into(ivAvatar);
                    } else if (currentUser.getPhotoUrl() != null) {
                        Glide.with(ProfileActivity.this)
                                .load(currentUser.getPhotoUrl())
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .into(ivAvatar);
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (tvProfileFullName != null && currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                    tvProfileFullName.setText(currentUser.getDisplayName());
                }
            }
        });
    }

    private void uploadAvatarImage(Uri imageUri) {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) return;

        Toast.makeText(this, "Đang tải ảnh đại diện lên...", Toast.LENGTH_SHORT).show();

        userRepository.uploadAvatar(currentUser.getUid(), imageUri, new UserRepository.UploadCallback() {
            @Override
            public void onSuccess(String avatarUrl) {
                Toast.makeText(ProfileActivity.this, "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show();
                if (ivAvatar != null) {
                    Glide.with(ProfileActivity.this)
                            .load(avatarUrl)
                            .placeholder(R.drawable.ic_person)
                            .into(ivAvatar);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ProfileActivity.this, "Tải ảnh đại diện thất bại: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleLogout() {
        authRepository.logout();
        Toast.makeText(this, "Đã đăng xuất khỏi BookShare.", Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
