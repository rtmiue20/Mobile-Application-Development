package vn.edu.ueh.rtmiue20.project.ui.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.repository.AuthRepository;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "BookSharePrefs";
    private static final String KEY_IDENTIFIER = "saved_identifier";

    private EditText etRecoveryEmail;
    private ImageView btnClearEmail;
    private LinearLayout btnSubmitRecovery;
    private ProgressBar pbSubmitLoading;
    private ImageView ivSubmitIcon;
    private TextView tvSubmitText;
    private View layoutSuccessBanner;
    private View btnBackToLogin;

    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainForgotPassword), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        authRepository = AuthRepository.getInstance();

        initViews();
        loadEmail();
        setupListeners();
    }

    private void initViews() {
        etRecoveryEmail = findViewById(R.id.etRecoveryEmail);
        btnClearEmail = findViewById(R.id.btnClearEmail);
        btnSubmitRecovery = findViewById(R.id.btnSubmitRecovery);
        pbSubmitLoading = findViewById(R.id.pbSubmitLoading);
        ivSubmitIcon = findViewById(R.id.ivSubmitIcon);
        tvSubmitText = findViewById(R.id.tvSubmitText);
        layoutSuccessBanner = findViewById(R.id.layoutSuccessBanner);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);
    }

    private void loadEmail() {
        String emailExtra = getIntent().getStringExtra("email");
        if (emailExtra != null && !emailExtra.trim().isEmpty()) {
            etRecoveryEmail.setText(emailExtra.trim());
            etRecoveryEmail.setSelection(etRecoveryEmail.getText().length());
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedEmail = prefs.getString(KEY_IDENTIFIER, "");
        if (!savedEmail.isEmpty()) {
            etRecoveryEmail.setText(savedEmail);
            etRecoveryEmail.setSelection(savedEmail.length());
        }
    }

    private void setupListeners() {
        btnClearEmail.setOnClickListener(v -> {
            etRecoveryEmail.setText("");
            etRecoveryEmail.requestFocus();
        });

        etRecoveryEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearEmail.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSubmitRecovery.setOnClickListener(v -> handleSendRecovery());
        btnBackToLogin.setOnClickListener(v -> finish());

        View btnHeaderBack = findViewById(R.id.btnHeaderBack);
        if (btnHeaderBack != null) btnHeaderBack.setOnClickListener(v -> finish());
    }

    private void handleSendRecovery() {
        String email = etRecoveryEmail.getText().toString().trim();

        if (email.isEmpty()) {
            etRecoveryEmail.setError("Vui lòng nhập địa chỉ Email tài khoản");
            etRecoveryEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRecoveryEmail.setError("Địa chỉ Email không hợp lệ");
            etRecoveryEmail.requestFocus();
            return;
        }

        btnSubmitRecovery.setEnabled(false);
        ivSubmitIcon.setVisibility(View.GONE);
        pbSubmitLoading.setVisibility(View.VISIBLE);
        tvSubmitText.setText(R.string.btn_sending);

        authRepository.sendPasswordResetEmail(email, new AuthRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                pbSubmitLoading.setVisibility(View.GONE);
                ivSubmitIcon.setVisibility(View.VISIBLE);
                tvSubmitText.setText(R.string.btn_resend_recovery);
                btnSubmitRecovery.setEnabled(true);
                layoutSuccessBanner.setVisibility(View.VISIBLE);

                Toast.makeText(ForgotPasswordActivity.this, "Đã gửi email hướng dẫn khôi phục tới " + email, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String errorMessage) {
                pbSubmitLoading.setVisibility(View.GONE);
                ivSubmitIcon.setVisibility(View.VISIBLE);
                tvSubmitText.setText(R.string.btn_send_recovery);
                btnSubmitRecovery.setEnabled(true);

                Toast.makeText(ForgotPasswordActivity.this, "Lỗi gửi email khôi phục: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
