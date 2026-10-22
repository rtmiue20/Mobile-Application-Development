package vn.edu.ueh.rtmiue20.project.ui.profile;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseUser;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.User;
import vn.edu.ueh.rtmiue20.project.data.repository.AuthRepository;
import vn.edu.ueh.rtmiue20.project.data.repository.UserRepository;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView btnBackEditProfile;
    private View btnSaveProfile;
    private View btnSaveProfileMain;
    private View btnCancelEditProfile;
    private EditText etEditFullName;
    private EditText etEditPhone;
    private EditText etEditBio;
    private TextView tvBioCounter;
    private TextView tvEditEmail;

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
        setupBioCounter();
        loadExistingData();
        setupListeners();
    }

    private void initViews() {
        btnBackEditProfile = findViewById(R.id.btnBackEditProfile);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnSaveProfileMain = findViewById(R.id.btnSaveProfileMain);
        btnCancelEditProfile = findViewById(R.id.btnCancelEditProfile);
        etEditFullName = findViewById(R.id.etEditFullName);
        etEditPhone = findViewById(R.id.etEditPhone);
        etEditBio = findViewById(R.id.etEditBio);
        tvBioCounter = findViewById(R.id.tvBioCounter);
        tvEditEmail = findViewById(R.id.tvEditEmail);
    }

    private void setupBioCounter() {
        if (etEditBio != null && tvBioCounter != null) {
            etEditBio.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    int len = s != null ? s.length() : 0;
                    tvBioCounter.setText(len + "/200");
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void loadExistingData() {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        if (tvEditEmail != null && currentUser.getEmail() != null) {
            tvEditEmail.setText(currentUser.getEmail());
        }

        userRepository.getUserProfile(currentUser.getUid(), new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (etEditFullName != null && user.getFullName() != null) etEditFullName.setText(user.getFullName());
                if (etEditPhone != null && user.getPhoneNumber() != null) etEditPhone.setText(user.getPhoneNumber());
                if (etEditBio != null && user.getBio() != null) etEditBio.setText(user.getBio());
            }

            @Override
            public void onError(String errorMessage) {
                if (etEditFullName != null && currentUser.getDisplayName() != null) {
                    etEditFullName.setText(currentUser.getDisplayName());
                }
            }
        });
    }

    private void setupListeners() {
        if (btnBackEditProfile != null) btnBackEditProfile.setOnClickListener(v -> finish());
        if (btnCancelEditProfile != null) btnCancelEditProfile.setOnClickListener(v -> finish());

        View.OnClickListener saveListener = v -> handleSaveProfile();
        if (btnSaveProfile != null) btnSaveProfile.setOnClickListener(saveListener);
        if (btnSaveProfileMain != null) btnSaveProfileMain.setOnClickListener(saveListener);
    }

    private void handleSaveProfile() {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) return;

        String fullName = etEditFullName != null ? etEditFullName.getText().toString().trim() : "";
        String phone = etEditPhone != null ? etEditPhone.getText().toString().trim() : "";
        String bio = etEditBio != null ? etEditBio.getText().toString().trim() : "";

        if (fullName.isEmpty()) {
            if (etEditFullName != null) {
                etEditFullName.setError("Vui lòng nhập họ và tên");
                etEditFullName.requestFocus();
            }
            return;
        }

        if (btnSaveProfile != null) btnSaveProfile.setEnabled(false);
        if (btnSaveProfileMain != null) btnSaveProfileMain.setEnabled(false);

        userRepository.updateUserProfile(currentUser.getUid(), fullName, phone, bio, new UserRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(EditProfileActivity.this, "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                if (btnSaveProfile != null) btnSaveProfile.setEnabled(true);
                if (btnSaveProfileMain != null) btnSaveProfileMain.setEnabled(true);
                Toast.makeText(EditProfileActivity.this, "Lỗi cập nhật: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
