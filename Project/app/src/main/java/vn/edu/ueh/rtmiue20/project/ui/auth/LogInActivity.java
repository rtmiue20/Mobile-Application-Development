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
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;

import android.os.CancellationSignal;
import android.util.Log;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.User;
import vn.edu.ueh.rtmiue20.project.data.repository.AuthRepository;
import vn.edu.ueh.rtmiue20.project.data.repository.UserRepository;
import vn.edu.ueh.rtmiue20.project.ui.admin.AdminDashboardActivity;
import vn.edu.ueh.rtmiue20.project.ui.home.HomeActivity;
import vn.edu.ueh.rtmiue20.project.util.ViewPressEffectHelper;
import vn.edu.ueh.rtmiue20.project.util.SigningInfoHelper;

public class LogInActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "BookSharePrefs";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_IDENTIFIER = "saved_identifier";

    private EditText etIdentifier;
    private EditText etPassword;
    private ImageView btnTogglePassword;
    private CheckBox cbRememberMe;
    private TextView tvForgotPassword;
    private LinearLayout btnLogin;
    private LinearLayout btnGoogleLogin;
    private LinearLayout btnGuestLogin;
    private TextView tvRegisterNow;
    private View layoutCommunityGuidelines;

    private boolean isPasswordVisible = false;
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
        setContentView(R.layout.activity_login);

        View mainView = findViewById(R.id.mainLogin);
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
        setupRegisterText();
        loadSavedCredentials();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedCredentials();
    }

    private void initViews() {
        etIdentifier = findViewById(R.id.etIdentifier);
        etPassword = findViewById(R.id.etPassword);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        btnGuestLogin = findViewById(R.id.btnGuestLogin);
        tvRegisterNow = findViewById(R.id.tvRegisterNow);
        layoutCommunityGuidelines = findViewById(R.id.layoutCommunityGuidelines);
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

        btnGoogleLogin.setEnabled(false);

        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        btnGoogleLogin.setEnabled(true);
                        handleGoogleSignInResponse(result);
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        btnGoogleLogin.setEnabled(true);
                        // Ghi log đầy đủ (kể cả trường hợp Cancellation) để chẩn đoán lỗi cấu hình SHA / OAuth client
                        Log.e("LogInActivity", "Google sign-in error [" + e.getClass().getSimpleName() + "] type=" + e.getType() + " msg=" + e.getMessage(), e);
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
    }

    /**
     * Luồng dự phòng khi Credential Manager thất bại (VD: "[16] Account reauth failed").
     * Google Sign-In cổ điển tự hiển thị màn hình xác thực lại tài khoản nếu Google yêu cầu,
     * nên xử lý được trường hợp tài khoản trên thiết bị cần đăng nhập lại.
     */
    private void startLegacyGoogleSignIn(String reason) {
        Log.w("LogInActivity", "Chuyển sang Google Sign-In dự phòng. Lý do: " + reason);
        btnGoogleLogin.setEnabled(false);
        // Đăng xuất phiên Google cũ của app để luôn hiện bộ chọn tài khoản
        legacyGoogleSignInClient.signOut().addOnCompleteListener(this, task ->
                legacyGoogleSignInLauncher.launch(legacyGoogleSignInClient.getSignInIntent()));
    }

    private void handleLegacyGoogleSignInResult(ActivityResult result) {
        btnGoogleLogin.setEnabled(true);
        if (result.getData() == null) {
            // Người dùng bấm Back / đóng màn hình chọn tài khoản
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
            Log.e("LogInActivity", "Google Sign-In dự phòng thất bại, code=" + code, e);
            if (code == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                return;
            }
            String hint;
            if (code == CommonStatusCodes.DEVELOPER_ERROR) {
                showSigningFingerprintDialog();
                return;
            } else if (code == CommonStatusCodes.NETWORK_ERROR) {
                hint = "Lỗi kết nối mạng (mã 7)";
            } else {
                hint = GoogleSignInStatusCodes.getStatusCodeString(code) + " (mã " + code + ")";
            }
            Toast.makeText(this, "Đăng nhập Google thất bại: " + hint, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Hiển thị SHA-1 / SHA-256 THỰC TẾ của APK đang chạy để so khớp với Firebase / Google Cloud
     * khi Google Sign-In trả về DEVELOPER_ERROR (mã 10).
     */
    private void showSigningFingerprintDialog() {
        String sha1 = SigningInfoHelper.getFingerprint(this, "SHA-1");
        String sha256 = SigningInfoHelper.getFingerprint(this, "SHA-256");
        Log.e("LogInActivity", "APK package=" + getPackageName() + " SHA-1=" + sha1 + " SHA-256=" + sha256);

        String message = "Google từ chối vì SHA của app chưa được đăng ký (mã 10).\n\n"
                + "Package:\n" + getPackageName() + "\n\n"
                + "SHA-1 thực tế:\n" + sha1 + "\n\n"
                + "SHA-256 thực tế:\n" + sha256 + "\n\n"
                + "→ Thêm 2 mã này vào Firebase > Cài đặt dự án > Thêm dấu vân tay.";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Chẩn đoán Google Sign-In")
                .setMessage(message)
                .setPositiveButton("Sao chép", (d, w) -> {
                    android.content.ClipboardManager cm =
                            (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("SHA",
                                "SHA-1: " + sha1 + "\nSHA-256: " + sha256));
                        Toast.makeText(this, "Đã sao chép SHA vào clipboard", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
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
                    Log.e("LogInActivity", "Parsing Google ID Token failed", e);
                    Toast.makeText(this, "Lỗi phân tích thông tin xác thực Google.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Loại thông tin xác thực Google không hỗ trợ.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Không nhận được phản hồi hợp lệ từ Google.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRegisterText() {
        String prefix = "Bạn chưa có tài khoản? ";
        String registerNowStr = getString(R.string.register_now);
        String fullText = prefix + registerNowStr;

        SpannableString spannableString = new SpannableString(fullText);
        int start = prefix.length();
        int end = fullText.length();

        int greenColor = ContextCompat.getColor(this, R.color.secondary_green);
        spannableString.setSpan(new ForegroundColorSpan(greenColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvRegisterNow.setText(spannableString);
    }

    private void loadSavedCredentials() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean rememberMe = prefs.getBoolean(KEY_REMEMBER_ME, false);
        cbRememberMe.setChecked(rememberMe);

        String savedIdentifier = prefs.getString(KEY_IDENTIFIER, "");
        if (!savedIdentifier.isEmpty()) {
            etIdentifier.setText(savedIdentifier);
            etIdentifier.setSelection(savedIdentifier.length());
        }
    }

    private void setupListeners() {
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        btnLogin.setOnClickListener(v -> handleLogin());

        btnGoogleLogin.setOnClickListener(v -> handleGoogleSignIn());
        btnGuestLogin.setOnClickListener(v -> {
            Toast.makeText(this, "Vào xem với tư cách Khách", Toast.LENGTH_SHORT).show();
            proceedToNextScreen(null);
        });

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LogInActivity.this, ForgotPasswordActivity.class);
            String currentInput = etIdentifier.getText().toString().trim();
            if (!currentInput.isEmpty()) {
                intent.putExtra("email", currentInput);
            }
            startActivity(intent);
        });

        tvRegisterNow.setOnClickListener(v -> {
            Intent intent = new Intent(LogInActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        layoutCommunityGuidelines.setOnClickListener(v -> 
            Toast.makeText(this, "Quy chuẩn Cộng đồng BookShare", Toast.LENGTH_SHORT).show()
        );
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility);
            isPasswordVisible = false;
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
            isPasswordVisible = true;
        }
        etPassword.setSelection(etPassword.getText().length());
    }

    private void handleLogin() {
        String identifier = etIdentifier.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (identifier.isEmpty()) {
            etIdentifier.setError("Vui lòng nhập Email tài khoản");
            etIdentifier.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Vui lòng nhập Mật khẩu");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        Toast.makeText(this, "Đang xử lý đăng nhập...", Toast.LENGTH_SHORT).show();

        authRepository.loginWithEmail(identifier, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                saveRememberMe(identifier);
                Toast.makeText(LogInActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                navigateToDashboard();
            }

            @Override
            public void onError(String errorMessage) {
                btnLogin.setEnabled(true);
                Toast.makeText(LogInActivity.this, "Lỗi đăng nhập: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void performGoogleAuth(String idToken) {
        Toast.makeText(this, "Đang xác thực bằng tài khoản Google...", Toast.LENGTH_SHORT).show();
        authRepository.loginWithGoogle(idToken, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                saveRememberMe(user.getEmail() != null ? user.getEmail() : "");
                Toast.makeText(LogInActivity.this, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show();
                navigateToDashboard();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(LogInActivity.this, "Lỗi đăng nhập Google: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveRememberMe(String identifier) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (cbRememberMe.isChecked()) {
            editor.putBoolean(KEY_REMEMBER_ME, true);
            editor.putString(KEY_IDENTIFIER, identifier);
        } else {
            editor.putBoolean(KEY_REMEMBER_ME, false);
            editor.remove(KEY_IDENTIFIER);
        }
        editor.apply();
    }

    private void navigateToDashboard() {
        FirebaseUser user = authRepository.getCurrentUser();
        if (user != null) {
            if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit().putString("user_fullname", user.getDisplayName()).apply();
            }
            UserRepository.getInstance().getUserProfile(
                    user.getUid(),
                    new UserRepository.UserCallback() {
                        @Override
                        public void onSuccess(User userProfile) {
                            if (userProfile != null && Boolean.TRUE.equals(userProfile.isMustChangePassword())) {
                                showForceChangePasswordDialog(user, userProfile);
                                return;
                            }
                            proceedToNextScreen(userProfile);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            proceedToNextScreen(null);
                        }
                    }
            );
        } else {
            proceedToNextScreen(null);
        }
    }

    /**
     * Bắt buộc thành viên đổi mật khẩu cá nhân khi tài khoản vừa được Admin cấp lại mật khẩu.
     */
    private void showForceChangePasswordDialog(FirebaseUser user, User userProfile) {
        if (isFinishing() || isDestroyed()) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_force_change_password, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextInputLayout tilForceNewPassword = dialogView.findViewById(R.id.tilForceNewPassword);
        TextInputEditText etForceNewPassword = dialogView.findViewById(R.id.etForceNewPassword);
        TextInputLayout tilForceConfirmPassword = dialogView.findViewById(R.id.tilForceConfirmPassword);
        TextInputEditText etForceConfirmPassword = dialogView.findViewById(R.id.etForceConfirmPassword);
        ProgressBar pbLoading = dialogView.findViewById(R.id.pbForceChangeLoading);
        TextView btnCancel = dialogView.findViewById(R.id.btnForceCancel);
        View btnSubmit = dialogView.findViewById(R.id.btnForceSubmit);
        TextView tvForceSubmitText = dialogView.findViewById(R.id.tvForceSubmitText);

        ViewPressEffectHelper.applyPressEffect(btnCancel, btnSubmit);

        btnCancel.setOnClickListener(v -> {
            authRepository.logout();
            dialog.dismiss();
            btnLogin.setEnabled(true);
            Toast.makeText(this, "Đã đăng xuất tài khoản.", Toast.LENGTH_SHORT).show();
        });

        btnSubmit.setOnClickListener(v -> {
            String newPass = etForceNewPassword.getText() != null ? etForceNewPassword.getText().toString().trim() : "";
            String confirmPass = etForceConfirmPassword.getText() != null ? etForceConfirmPassword.getText().toString().trim() : "";

            if (newPass.isEmpty()) {
                tilForceNewPassword.setError(getString(R.string.err_password_empty));
                etForceNewPassword.requestFocus();
                return;
            }

            if (newPass.length() < 8) {
                tilForceNewPassword.setError(getString(R.string.err_password_min_length));
                etForceNewPassword.requestFocus();
                return;
            }

            if (confirmPass.isEmpty()) {
                tilForceConfirmPassword.setError(getString(R.string.err_confirm_password_empty));
                etForceConfirmPassword.requestFocus();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                tilForceConfirmPassword.setError(getString(R.string.err_password_mismatch));
                etForceConfirmPassword.requestFocus();
                return;
            }

            btnSubmit.setEnabled(false);
            btnCancel.setEnabled(false);
            etForceNewPassword.setEnabled(false);
            etForceConfirmPassword.setEnabled(false);
            pbLoading.setVisibility(View.VISIBLE);
            tvForceSubmitText.setText(R.string.btn_processing);

            user.updatePassword(newPass)
                    .addOnSuccessListener(unused -> {
                        UserRepository.getInstance().clearMustChangePasswordFlag(user.getUid(), new UserRepository.UpdateCallback() {
                            @Override
                            public void onSuccess() {
                                if (isFinishing() || isDestroyed()) return;
                                dialog.dismiss();
                                Toast.makeText(LogInActivity.this, R.string.msg_force_change_password_success, Toast.LENGTH_LONG).show();
                                proceedToNextScreen(userProfile);
                            }

                            @Override
                            public void onError(String errorMsg) {
                                if (isFinishing() || isDestroyed()) return;
                                dialog.dismiss();
                                Toast.makeText(LogInActivity.this, R.string.msg_force_change_password_success, Toast.LENGTH_LONG).show();
                                proceedToNextScreen(userProfile);
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        if (isFinishing() || isDestroyed()) return;
                        btnSubmit.setEnabled(true);
                        btnCancel.setEnabled(true);
                        etForceNewPassword.setEnabled(true);
                        etForceConfirmPassword.setEnabled(true);
                        pbLoading.setVisibility(View.GONE);
                        tvForceSubmitText.setText(R.string.btn_force_change_password);
                        tilForceNewPassword.setError(e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Không thể cập nhật mật khẩu.");
                        Toast.makeText(LogInActivity.this, "Lỗi: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        dialog.show();
    }

    private void proceedToNextScreen(User userProfile) {
        try {
            Intent intent;
            if (userProfile != null && "admin".equalsIgnoreCase(userProfile.getRole())) {
                intent = new Intent(LogInActivity.this, AdminDashboardActivity.class);
            } else {
                intent = new Intent(LogInActivity.this, HomeActivity.class);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e("LogInActivity", "Error navigating to dashboard", e);
            btnLogin.setEnabled(true);
            Toast.makeText(LogInActivity.this, "Lỗi chuyển màn hình: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
