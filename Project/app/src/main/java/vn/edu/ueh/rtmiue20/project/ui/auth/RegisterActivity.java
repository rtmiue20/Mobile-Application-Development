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

import android.os.CancellationSignal;
import android.util.Log;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.Task;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
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
    private CredentialManager credentialManager;

    // Luồng Google Sign-In dự phòng (dùng khi Credential Manager lỗi, ví dụ "[16] Account reauth failed")
    private GoogleSignInClient legacyGoogleSignInClient;
    private final ActivityResultLauncher<Intent> legacyGoogleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    this::handleLegacyGoogleSignInResult);

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
        credentialManager = CredentialManager.create(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        legacyGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void handleGoogleSignIn() {
        String webClientId = getString(R.string.default_web_client_id);
        if (webClientId.contains("exampleclientid") || webClientId.contains("1234567890")) {
            Toast.makeText(this, "Lỗi cấu hình Google Sign-In: Bạn chưa thay thế Web Client ID thật từ Firebase Console!", Toast.LENGTH_LONG).show();
            return;
        }

        GetSignInWithGoogleOption signInWithGoogleOption = new GetSignInWithGoogleOption.Builder(webClientId)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build();

        btnGoogleRegister.setEnabled(false);

        try {
            credentialManager.getCredentialAsync(
                    this,
                    request,
                    new CancellationSignal(),
                    ContextCompat.getMainExecutor(this),
                    new CredentialManagerCallback<>() {
                        @Override
                        public void onResult(GetCredentialResponse result) {
                            btnGoogleRegister.setEnabled(true);
                            handleGoogleSignInResponse(result);
                        }

                        @Override
                        public void onError(@NonNull GetCredentialException e) {
                            btnGoogleRegister.setEnabled(true);
                            Log.e("RegisterActivity", "Google sign-in error [" + e.getClass().getSimpleName() + "] type=" + e.getType() + " msg=" + e.getMessage(), e);
                            if (e instanceof GetCredentialCancellationException) {
                                String reason = e.getMessage() != null ? e.getMessage() : "";
                                // Người dùng chủ động đóng hộp thoại -> không làm gì
                                if (reason.toLowerCase().contains("cancelled by the user")) {
                                    return;
                                }
                                // Các trường hợp còn lại (VD: "[16] Account reauth failed") -> chuyển sang luồng dự phòng
                                startLegacyGoogleSignIn(reason);
                                return;
                            }
                            // Lỗi khác của Credential Manager (VD: NoCredentialException) -> thử luồng dự phòng
                            startLegacyGoogleSignIn(e.getMessage());
                        }
                    }
            );
        } catch (Exception e) {
            btnGoogleRegister.setEnabled(true);
            Log.e("RegisterActivity", "Lỗi CredentialManager: " + e.getMessage(), e);
            startLegacyGoogleSignIn(e.getMessage());
        }
    }

    /**
     * Luồng dự phòng khi Credential Manager thất bại (VD: "[16] Account reauth failed").
     * Google Sign-In cổ điển tự hiển thị màn hình xác thực lại tài khoản nếu Google yêu cầu.
     */
    private void startLegacyGoogleSignIn(String reason) {
        Log.w("RegisterActivity", "Chuyển sang Google Sign-In dự phòng. Lý do: " + reason);
        btnGoogleRegister.setEnabled(false);
        legacyGoogleSignInClient.signOut().addOnCompleteListener(this, task ->
                legacyGoogleSignInLauncher.launch(legacyGoogleSignInClient.getSignInIntent()));
    }

    private void handleLegacyGoogleSignInResult(ActivityResult result) {
        btnGoogleRegister.setEnabled(true);
        if (result.getData() == null) {
            return;
        }
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            String idToken = account != null ? account.getIdToken() : null;
            if (idToken != null && !idToken.isEmpty()) {
                performGoogleAuth(idToken);
            } else {
                Toast.makeText(this, "Không thể lấy ID token từ Google.", Toast.LENGTH_LONG).show();
            }
        } catch (ApiException e) {
            int code = e.getStatusCode();
            Log.e("RegisterActivity", "Google Sign-In dự phòng thất bại, code=" + code, e);
            if (code == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                return;
            }
            String hint;
            if (code == CommonStatusCodes.DEVELOPER_ERROR) {
                hint = "Sai cấu hình SHA-1 / package / Web Client ID trên Firebase (mã 10)";
            } else if (code == CommonStatusCodes.NETWORK_ERROR) {
                hint = "Lỗi kết nối mạng (mã 7)";
            } else {
                hint = GoogleSignInStatusCodes.getStatusCodeString(code) + " (mã " + code + ")";
            }
            Toast.makeText(this, "Đăng ký Google thất bại: " + hint, Toast.LENGTH_LONG).show();
        }
    }

    private void handleGoogleSignInResponse(GetCredentialResponse result) {
        Credential credential = result.getCredential();
        if (credential instanceof CustomCredential) {
            CustomCredential customCredential = (CustomCredential) credential;
            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(customCredential.getType())) {
                try {
                    GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(customCredential.getData());
                    String idToken = googleIdTokenCredential.getIdToken();
                    if (!idToken.isEmpty()) {
                        performGoogleAuth(idToken);
                    } else {
                        Toast.makeText(this, "Không thể lấy ID token từ Google.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e("RegisterActivity", "Parsing Google ID Token failed", e);
                    Toast.makeText(this, "Lỗi phân tích thông tin xác thực Google.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Loại thông tin xác thực Google không hỗ trợ.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Không nhận được phản hồi hợp lệ từ Google.", Toast.LENGTH_SHORT).show();
        }
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

        btnGoogleRegister.setOnClickListener(v -> handleGoogleSignIn());

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
