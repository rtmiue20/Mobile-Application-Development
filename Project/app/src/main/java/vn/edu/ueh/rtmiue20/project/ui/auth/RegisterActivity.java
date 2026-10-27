package vn.edu.ueh.rtmiue20.project.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.repository.AuthRepository;
import vn.edu.ueh.rtmiue20.project.ui.home.HomeActivity;

import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "BookSharePrefs";
    private static final String KEY_IDENTIFIER = "saved_identifier";

    private EditText etFullName;
    private EditText etEmail;
    private EditText etRegisterPassword;
    private EditText etConfirmPassword;
    private ImageView btnToggleRegisterPassword;
    private ImageView btnToggleConfirmPassword;
    private CheckBox cbTerms;
    private LinearLayout btnRegisterAccount;
    private LinearLayout btnGoogleRegister;
    private TextView tvLoginNow;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    private AuthRepository authRepository;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        View mainView = findViewById(R.id.mainRegister);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        authRepository = AuthRepository.getInstance();

        initViews();
        setupGoogleSignIn();
        setupFormattedTexts();
        setupListeners();
    }

    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnToggleRegisterPassword = findViewById(R.id.btnToggleRegisterPassword);
        btnToggleConfirmPassword = findViewById(R.id.btnToggleConfirmPassword);
        cbTerms = findViewById(R.id.cbTerms);
        btnRegisterAccount = findViewById(R.id.btnRegisterAccount);
        btnGoogleRegister = findViewById(R.id.btnGoogleRegister);
        tvLoginNow = findViewById(R.id.tvLoginNow);
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null && account.getIdToken() != null) {
                                performGoogleAuth(account.getIdToken());
                            } else {
                                Toast.makeText(this, "Không thể lấy thông tin xác thực từ Google.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (ApiException e) {
                            String msg;
                            if (e.getStatusCode() == 10 || e.getStatusCode() == 12500) {
                                msg = "Đăng ký Google thất bại: Chưa cấu hình SHA-1 / Google Web Client ID trong Firebase Console.";
                            } else {
                                msg = "Đăng ký Google thất bại (" + e.getStatusCode() + "): " + (e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Lỗi xác thực Google.");
                            }
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }

    private void setupFormattedTexts() {
        int greenColor = ContextCompat.getColor(this, R.color.secondary_green);

        String prefix = "Đã có tài khoản? ";
        String loginNowStr = getString(R.string.login_now);
        String fullLoginText = prefix + loginNowStr;

        SpannableString loginSpannable = new SpannableString(fullLoginText);
        int loginStart = prefix.length();
        int loginEnd = fullLoginText.length();

        loginSpannable.setSpan(new ForegroundColorSpan(greenColor), loginStart, loginEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        loginSpannable.setSpan(new StyleSpan(Typeface.BOLD), loginStart, loginEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        loginSpannable.setSpan(new UnderlineSpan(), loginStart, loginEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvLoginNow.setText(loginSpannable);
    }

    private void setupListeners() {
        btnToggleRegisterPassword.setOnClickListener(v -> togglePasswordVisibility());
        btnToggleConfirmPassword.setOnClickListener(v -> toggleConfirmPasswordVisibility());

        btnRegisterAccount.setOnClickListener(v -> handleRegistration());

        btnGoogleRegister.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        tvLoginNow.setOnClickListener(v -> finish());
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            etRegisterPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnToggleRegisterPassword.setImageResource(R.drawable.ic_visibility);
            isPasswordVisible = false;
        } else {
            etRegisterPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnToggleRegisterPassword.setImageResource(R.drawable.ic_visibility_off);
            isPasswordVisible = true;
        }
        etRegisterPassword.setSelection(etRegisterPassword.getText().length());
    }

    private void toggleConfirmPasswordVisibility() {
        if (isConfirmPasswordVisible) {
            etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnToggleConfirmPassword.setImageResource(R.drawable.ic_visibility);
            isConfirmPasswordVisible = false;
        } else {
            etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnToggleConfirmPassword.setImageResource(R.drawable.ic_visibility_off);
            isConfirmPasswordVisible = true;
        }
        etConfirmPassword.setSelection(etConfirmPassword.getText().length());
    }

    private void handleRegistration() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etRegisterPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (fullName.isEmpty()) {
            etFullName.setError("Vui lòng nhập Họ và tên");
            etFullName.requestFocus();
            return;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return;
        }

        if (password.length() < 8) {
            etRegisterPassword.setError("Mật khẩu phải chứa ít nhất 8 ký tự");
            etRegisterPassword.requestFocus();
            return;
        }

        if (!Objects.equals(password, confirmPassword)) {
            etConfirmPassword.setError("Mật khẩu xác nhận không trùng khớp");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Vui lòng đồng ý với Điều khoản dịch vụ và Quy chuẩn cộng đồng để tiếp tục", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegisterAccount.setEnabled(false);
        Toast.makeText(this, "Đang khởi tạo tài khoản...", Toast.LENGTH_SHORT).show();

        authRepository.registerWithEmail(email, password, fullName, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit()
                        .putString(KEY_IDENTIFIER, email)
                        .putString("user_fullname", fullName)
                        .apply();

                captureUserLocationIfPermitted(user.getUid());
                Toast.makeText(RegisterActivity.this, "Đăng ký tài khoản thành công!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                btnRegisterAccount.setEnabled(true);
                new androidx.appcompat.app.AlertDialog.Builder(RegisterActivity.this)
                        .setTitle("Thông báo đăng ký")
                        .setMessage(errorMessage)
                        .setPositiveButton(errorMessage != null && errorMessage.contains("đã được đăng ký") ? "Đăng nhập ngay" : "Đã hiểu", (dialog, which) -> {
                            if (errorMessage != null && errorMessage.contains("đã được đăng ký")) {
                                finish();
                            }
                        })
                        .show();
            }
        });
    }

    private void performGoogleAuth(String idToken) {
        Toast.makeText(this, "Đang xác thực bằng tài khoản Google...", Toast.LENGTH_SHORT).show();
        authRepository.loginWithGoogle(idToken, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                captureUserLocationIfPermitted(user.getUid());
                Toast.makeText(RegisterActivity.this, "Đăng ký Google thành công!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(RegisterActivity.this, "Lỗi đăng ký Google: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void captureUserLocationIfPermitted(String uid) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try {
                com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
                        .getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                        .addOnSuccessListener(loc -> {
                            if (loc != null) {
                                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                                prefs.edit()
                                        .putFloat("user_lat", (float) loc.getLatitude())
                                        .putFloat("user_lng", (float) loc.getLongitude())
                                        .apply();
                                vn.edu.ueh.rtmiue20.project.data.repository.UserRepository.getInstance()
                                        .updateUserLocation(uid, loc.getLatitude(), loc.getLongitude(), null);
                            }
                        });
            } catch (SecurityException ignored) {}
        }
    }
}
