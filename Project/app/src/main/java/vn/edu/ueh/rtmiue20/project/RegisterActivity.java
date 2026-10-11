package vn.edu.ueh.rtmiue20.project;

import android.content.Context;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRegister), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
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

    private void setupFormattedTexts() {
        int greenColor = ContextCompat.getColor(this, R.color.secondary_green);

        // Format Login Link Footer Text
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
        // Password toggles
        btnToggleRegisterPassword.setOnClickListener(v -> togglePasswordVisibility());
        btnToggleConfirmPassword.setOnClickListener(v -> toggleConfirmPasswordVisibility());

        // Register action
        btnRegisterAccount.setOnClickListener(v -> handleRegistration());

        // Google register action
        btnGoogleRegister.setOnClickListener(v -> 
            Toast.makeText(this, "Đang đăng ký bằng tài khoản Google...", Toast.LENGTH_SHORT).show()
        );

        // Login Link action -> Finish activity to return to MainActivity
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

        // Save registered email to SharedPreferences so Login screen can pre-fill
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_IDENTIFIER, email).apply();

        Toast.makeText(this, "Đăng ký tài khoản thành công!\nChào mừng " + fullName + " đến với BookShare", Toast.LENGTH_LONG).show();

        // Finish activity and return to Login
        finish();
    }
}
