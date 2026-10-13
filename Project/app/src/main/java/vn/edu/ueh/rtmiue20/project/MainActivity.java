package vn.edu.ueh.rtmiue20.project;

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

import vn.edu.ueh.rtmiue20.project.data.repository.AuthRepository;

public class MainActivity extends AppCompatActivity {

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
    private TextView tvRegisterNow;
    private View layoutCommunityGuidelines;

    private boolean isPasswordVisible = false;
    private AuthRepository authRepository;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        authRepository = AuthRepository.getInstance();

        // Check if user is already logged in
        if (authRepository.isLoggedIn()) {
            navigateToProfile();
            return;
        }

        initViews();
        setupGoogleSignIn();
        setupRegisterText();
        loadSavedCredentials();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (authRepository.isLoggedIn()) {
            navigateToProfile();
            return;
        }
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
        tvRegisterNow = findViewById(R.id.tvRegisterNow);
        layoutCommunityGuidelines = findViewById(R.id.layoutCommunityGuidelines);
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
                            }
                        } catch (ApiException e) {
                            Toast.makeText(this, "Đăng nhập Google thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
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

        btnGoogleLogin.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ForgotPasswordActivity.class);
            String currentInput = etIdentifier.getText().toString().trim();
            if (!currentInput.isEmpty()) {
                intent.putExtra("email", currentInput);
            }
            startActivity(intent);
        });

        tvRegisterNow.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
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
                Toast.makeText(MainActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                navigateToProfile();
            }

            @Override
            public void onError(String errorMessage) {
                btnLogin.setEnabled(true);
                Toast.makeText(MainActivity.this, "Lỗi đăng nhập: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void performGoogleAuth(String idToken) {
        Toast.makeText(this, "Đang xác thực bằng tài khoản Google...", Toast.LENGTH_SHORT).show();
        authRepository.loginWithGoogle(idToken, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Toast.makeText(MainActivity.this, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show();
                navigateToProfile();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this, "Lỗi đăng nhập Google: " + errorMessage, Toast.LENGTH_LONG).show();
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

    private void navigateToProfile() {
        Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
        startActivity(intent);
        finish();
    }
}
