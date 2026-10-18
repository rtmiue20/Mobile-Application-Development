package vn.edu.ueh.rtmiue20.project.ui.book;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.Book;
import vn.edu.ueh.rtmiue20.project.data.repository.AuthRepository;
import vn.edu.ueh.rtmiue20.project.data.repository.BookRepository;
import vn.edu.ueh.rtmiue20.project.ui.home.BookViewModel;

public class CreatePostActivity extends AppCompatActivity {

    private ImageView btnCancel, ivPreviewImage;
    private MaterialCardView cardAddImage;
    private EditText etTitle, etAuthor, etDescription;
    private Spinner spinnerCategory;
    private RadioGroup rgCondition, rgSharingType;
    private Button btnSubmit;

    private Uri selectedImageUri = null;
    private BookViewModel bookViewModel;
    private AuthRepository authRepository;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    ivPreviewImage.setImageURI(selectedImageUri);
                    ivPreviewImage.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT));
                    ivPreviewImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_post);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainCreatePost), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bookViewModel = new ViewModelProvider(this).get(BookViewModel.class);
        authRepository = AuthRepository.getInstance();

        initViews();
        setupSpinner();
        setupListeners();
    }

    private void initViews() {
        btnCancel = findViewById(R.id.btnCancel);
        cardAddImage = findViewById(R.id.cardAddImage);
        ivPreviewImage = findViewById(R.id.ivPreviewImage);
        etTitle = findViewById(R.id.etTitle);
        etAuthor = findViewById(R.id.etAuthor);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        rgCondition = findViewById(R.id.rgCondition);
        rgSharingType = findViewById(R.id.rgSharingType);
        etDescription = findViewById(R.id.etDescription);
        btnSubmit = findViewById(R.id.btnSubmit);
    }

    private void setupSpinner() {
        String[] categories = {"Giáo trình", "Văn học", "Kỹ năng", "Kinh tế", "Khác"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupListeners() {
        btnCancel.setOnClickListener(v -> finish());

        cardAddImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        btnSubmit.setOnClickListener(v -> submitPost());
    }

    private void submitPost() {
        String title = etTitle.getText().toString().trim();
        String author = etAuthor.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (title.isEmpty() || author.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên sách và tác giả", Toast.LENGTH_SHORT).show();
            return;
        }

        String category = spinnerCategory.getSelectedItem().toString();

        String condition = "Mới";
        int checkedCondId = rgCondition.getCheckedRadioButtonId();
        if (checkedCondId == R.id.rbConditionGood) condition = "Tốt";
        else if (checkedCondId == R.id.rbConditionOld) condition = "Cũ vừa";
        else condition = "Mới 99%";

        String sharingType = "Tặng miễn phí";
        int checkedShareId = rgSharingType.getCheckedRadioButtonId();
        if (checkedShareId == R.id.rbShareExchange) sharingType = "Đổi sách";
        else if (checkedShareId == R.id.rbShareFee) sharingType = "Phí tượng trưng";

        // Append sharing type to description temporarily, or we could add a field later.
        String finalDesc = description + "\n\nHình thức: " + sharingType;
        String finalCondition = condition;

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang đăng...");

        if (selectedImageUri != null) {
            bookViewModel.uploadBookImage(selectedImageUri, new BookRepository.UploadImageCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    saveBookToDb(title, author, category, finalCondition, finalDesc, imageUrl);
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(CreatePostActivity.this, "Lỗi tải ảnh: " + errorMessage, Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Tiếp tục & Xem trước");
                }
            });
        } else {
            saveBookToDb(title, author, category, finalCondition, finalDesc, "");
        }
    }

    private void saveBookToDb(String title, String author, String category, String condition, String description, String imageUrl) {
        String ownerId = authRepository.getCurrentUserId();
        if (ownerId == null) ownerId = "anonymous";

        Book book = new Book("", title, author, description, imageUrl, ownerId, System.currentTimeMillis(), category, condition, "available");

        bookViewModel.saveBook(book, new BookRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(CreatePostActivity.this, "Đăng sách thành công!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(CreatePostActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Tiếp tục & Xem trước");
            }
        });
    }
}
