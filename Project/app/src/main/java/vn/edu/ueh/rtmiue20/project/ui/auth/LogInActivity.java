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
                        if (e instanceof GetCredentialCancellationException) {
                            return;
                        }
                        Log.e("LogInActivity", "Google sign-in error", e);
                        String msg = "Đăng nhập Google thất bại: " + (e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Lỗi xác thực Google.");
                        Toast.makeText(LogInActivity.this, msg, Toast.LENGTH_LONG).show();
                    }
                }
        );
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
