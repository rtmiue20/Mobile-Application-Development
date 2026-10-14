package vn.edu.ueh.rtmiue20.project;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class BookDetailActivity extends AppCompatActivity {

    // ==========================================
    // KHAI BÁO BIẾN (FIELDS)
    // ==========================================

    // Khai báo chung
    private String bookId;

    // Biến cho FR08 (Bình luận)
    private RecyclerView rvComments;
    private EditText etComment;
    private ImageView btnSendComment;
    private CommentAdapter commentAdapter;
    private CommentViewModel commentViewModel;

    // Biến cho FR09 (Yêu thích)
    private ImageView btnFavorite;
    private FavoriteViewModel favoriteViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_book_detail);

        // Thiết lập giao diện tràn viền (KHÔNG DÙNG LAMBDA)
        View mainView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            }
        });

        // Tạm thời hard-code bookId để test trên nhánh của Vương.
        // Khi gộp code với Phúc, đổi thành: bookId = getIntent().getStringExtra("bookId");
        bookId = "mock_book_test_123";

        // Ánh xạ View cho Bình luận (FR08)
        rvComments = findViewById(R.id.rv_comments);
        etComment = findViewById(R.id.et_comment);
        btnSendComment = findViewById(R.id.btn_send_comment);
        commentViewModel = new ViewModelProvider(this).get(CommentViewModel.class);

        // Ánh xạ View cho Yêu thích (FR09)
        btnFavorite = findViewById(R.id.btn_favorite);
        favoriteViewModel = new ViewModelProvider(this).get(FavoriteViewModel.class);

        // Gọi các hàm khởi tạo của FR08
        initRecyclerView();
        observeViewModel();
        setupListeners();

        // Gọi các hàm khởi tạo của FR09
        observeFavorite();
        setupFavoriteListener();
    }

    // ==========================================
    // PHẦN LOGIC CỦA TÍNH NĂNG BÌNH LUẬN (FR08)
    // ==========================================

    private void initRecyclerView() {
        commentAdapter = new CommentAdapter();
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);
        rvComments.setNestedScrollingEnabled(false);
    }

    private void observeViewModel() {
        if (bookId == null) {
            return;
        }

        commentViewModel.getComments(bookId).observe(this, new Observer<List<Comment>>() {
            @Override
            public void onChanged(List<Comment> comments) {
                commentAdapter.setComments(comments);
            }
        });

        commentViewModel.getAddSuccess().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                if (success != null && success) {
                    etComment.setText("");
                    btnSendComment.setEnabled(true);
                    commentViewModel.resetAddSuccess();
                }
            }
        });

        commentViewModel.getError().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String message) {
                if (message != null) {
                    btnSendComment.setEnabled(true);
                    Toast.makeText(BookDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    commentViewModel.resetError();
                }
            }
        });
    }

    private void setupListeners() {
        btnSendComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = etComment.getText().toString().trim();

                if (content.isEmpty()) {
                    Toast.makeText(BookDetailActivity.this,
                            getString(R.string.msg_comment_empty), Toast.LENGTH_SHORT).show();
                    return;
                }

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                    Toast.makeText(BookDetailActivity.this,
                            getString(R.string.msg_comment_need_login), Toast.LENGTH_SHORT).show();
                    return;
                }

                String userName = user.getDisplayName();
                if (userName == null || userName.trim().isEmpty()) {
                    userName = user.getEmail();
                }
                if (userName == null || userName.trim().isEmpty()) {
                    userName = "Người dùng BookShare";
                }

                Comment comment = new Comment(
                        null,
                        user.getUid(),
                        userName,
                        content,
                        System.currentTimeMillis());

                btnSendComment.setEnabled(false);
                commentViewModel.addComment(bookId, comment);
            }
        });
    }

    // ==========================================
    // PHẦN LOGIC CỦA TÍNH NĂNG YÊU THÍCH (FR09)
    // ==========================================

    private void observeFavorite() {
        if (bookId == null) {
            return;
        }

        favoriteViewModel.getIsFavorited(bookId).observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isFavorited) {
                if (isFavorited != null && isFavorited) {
                    btnFavorite.setImageResource(R.drawable.ic_heart_filled);
                } else {
                    btnFavorite.setImageResource(R.drawable.ic_heart_outline);
                }
            }
        });

        favoriteViewModel.getError().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String message) {
                if (message != null) {
                    Toast.makeText(BookDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    favoriteViewModel.resetError();
                }
            }
        });
    }

    private void setupFavoriteListener() {
        btnFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bookId == null) {
                    return;
                }
                favoriteViewModel.toggleFavorite(bookId);
            }
        });
    }
}