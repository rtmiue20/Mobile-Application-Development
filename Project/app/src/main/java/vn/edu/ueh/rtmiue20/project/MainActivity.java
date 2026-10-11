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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

        initViews();
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
        tvRegisterNow = findViewById(R.id.tvRegisterNow);
        layoutCommunityGuidelines = findViewById(R.id.layoutCommunityGuidelines);
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
        // Toggle Ẩn/Hiện Mật khẩu
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        // Đăng nhập
        btnLogin.setOnClickListener(v -> handleLogin());

        // Đăng nhập bằng Google
        btnGoogleLogin.setOnClickListener(v -> 
            Toast.makeText(this, "Đang đăng nhập bằng Google...", Toast.LENGTH_SHORT).show()
        );

        // Quên mật khẩu
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ForgotPasswordActivity.class);
            String currentInput = etIdentifier.getText().toString().trim();
            if (!currentInput.isEmpty()) {
                intent.putExtra("email", currentInput);
            }
            startActivity(intent);
        });

        // Chuyển sang Đăng ký
        tvRegisterNow.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Quy chuẩn Cộng đồng
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
            etIdentifier.setError("Vui lòng nhập Email hoặc Tên đăng nhập");
            etIdentifier.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Vui lòng nhập Mật khẩu");
            etPassword.requestFocus();
            return;
        }

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

        Toast.makeText(this, "Đăng nhập thành công!\nChào mừng " + identifier + " quay lại BookShare", Toast.LENGTH_LONG).show();
    }
}
