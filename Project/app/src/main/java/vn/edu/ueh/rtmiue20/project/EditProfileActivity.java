package vn.edu.ueh.rtmiue20.project;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseUser;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView btnBackEditProfile;
    private EditText etEditFullName;
    private EditText etEditPhone;
    private EditText etEditBio;
    private LinearLayout btnSaveProfile;

    private AuthRepository authRepository;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainEditProfile), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        authRepository = AuthRepository.getInstance();
        userRepository = UserRepository.getInstance();

        initViews();
        loadExistingData();
        setupListeners();
    }

    private void initViews() {
        btnBackEditProfile = findViewById(R.id.btnBackEditProfile);
        etEditFullName = findViewById(R.id.etEditFullName);
        etEditPhone = findViewById(R.id.etEditPhone);
        etEditBio = findViewById(R.id.etEditBio);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
    }

    private void loadExistingData() {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        userRepository.getUserProfile(currentUser.getUid(), new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user.getFullName() != null) etEditFullName.setText(user.getFullName());
                if (user.getPhoneNumber() != null) etEditPhone.setText(user.getPhoneNumber());
                if (user.getBio() != null) etEditBio.setText(user.getBio());
            }

            @Override
            public void onError(String errorMessage) {
                if (currentUser.getDisplayName() != null) {
                    etEditFullName.setText(currentUser.getDisplayName());
                }
            }
        });
    }

    private void setupListeners() {
        btnBackEditProfile.setOnClickListener(v -> finish());
        btnSaveProfile.setOnClickListener(v -> handleSaveProfile());
    }

    private void handleSaveProfile() {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) return;

        String fullName = etEditFullName.getText().toString().trim();
        String phone = etEditPhone.getText().toString().trim();
        String bio = etEditBio.getText().toString().trim();

        if (fullName.isEmpty()) {
            etEditFullName.setError("Vui lòng nhập họ và tên");
            etEditFullName.requestFocus();
            return;
        }

        btnSaveProfile.setEnabled(false);

        userRepository.updateUserProfile(currentUser.getUid(), fullName, phone, bio, new UserRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(EditProfileActivity.this, "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                btnSaveProfile.setEnabled(true);
                Toast.makeText(EditProfileActivity.this, "Lỗi cập nhật: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
