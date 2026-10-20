package vn.edu.ueh.rtmiue20.project.ui.book;

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.Book;
import vn.edu.ueh.rtmiue20.project.data.model.Comment;
import vn.edu.ueh.rtmiue20.project.data.model.User;
import vn.edu.ueh.rtmiue20.project.data.repository.BookRepository;
import vn.edu.ueh.rtmiue20.project.data.repository.UserRepository;
import vn.edu.ueh.rtmiue20.project.ui.favorite.FavoriteViewModel;
import vn.edu.ueh.rtmiue20.project.ui.home.BookViewModel;

public class BookDetailActivity extends AppCompatActivity {

    // ==========================================
    // KHAI BÁO BIẾN (FIELDS)
    // ==========================================

    // Khai báo chung
    private String bookId;
    private BookViewModel bookViewModel;
    private UserRepository userRepository;

    // View Chi tiết sách
    private ImageView ivBookCover, ivOwnerAvatar, btnBack;
    private TextView tvTitle, tvAuthor, tvCategory, tvCondition, tvDescription, tvOwnerName;
    private LinearLayout layoutOwnerActions;
    private Button btnEdit, btnDelete;

    // Biến cho FR08 (Bình luận)
    private RecyclerView rvComments;
    private EditText etComment;
    private ImageView btnSendComment;
    private CommentAdapter commentAdapter;
    private CommentViewModel commentViewModel;

    // Biến cho FR09 (Yêu thích)
    private ImageView btnFavorite;
    private FavoriteViewModel favoriteViewModel;

    // Biến cho FR12 (Offline Banner)
    private TextView tvOfflineBanner;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    // Biến cho FR10 (Yêu cầu nhận sách)
    private LinearLayout btnRequestBook;
    private vn.edu.ueh.rtmiue20.project.ui.request.RequestViewModel requestViewModel;
    private String currentBookTitle = "Sách không tên";
    private String currentOwnerId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_book_detail);

        // Thiết lập giao diện tràn viền
        View mainView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            }
        });

        bookId = getIntent().getStringExtra("bookId");
        if (bookId == null || bookId.isEmpty()) {
            bookId = "mock_book_test_123";
        }

        // Khởi tạo Views chung
        initViews();

        bookViewModel = new ViewModelProvider(this).get(BookViewModel.class);
        userRepository = UserRepository.getInstance();

        // Ánh xạ View cho Bình luận (FR08)
        rvComments = findViewById(R.id.rvComments);
        etComment = findViewById(R.id.et_comment);
        btnSendComment = findViewById(R.id.btn_send_comment);
        commentViewModel = new ViewModelProvider(this).get(CommentViewModel.class);

        // Ánh xạ View cho Yêu thích (FR09)
        btnFavorite = findViewById(R.id.btn_favorite);
        favoriteViewModel = new ViewModelProvider(this).get(FavoriteViewModel.class);

        // Ánh xạ View cho Yêu cầu nhận sách (FR10)
        btnRequestBook = findViewById(R.id.btn_request_book);
        requestViewModel = new ViewModelProvider(this).get(vn.edu.ueh.rtmiue20.project.ui.request.RequestViewModel.class);

        // Gọi các hàm khởi tạo
        initRecyclerView();
        observeViewModel();
        setupListeners();
        observeFavorite();
        setupFavoriteListener();
        loadBookData();

        // --- BẮT SỰ KIỆN MẠNG CHO FR12 ---
        tvOfflineBanner = findViewById(R.id.tvOfflineBanner);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (tvOfflineBanner != null) {
            tvOfflineBanner.setVisibility(isOnline() ? View.GONE : View.VISIBLE);
        }

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (tvOfflineBanner != null) {
                            tvOfflineBanner.setVisibility(View.GONE);
                        }
                    }
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (tvOfflineBanner != null) {
                            tvOfflineBanner.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        };

        if (connectivityManager != null) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }
    }

    private void initViews() {
        ivBookCover = findViewById(R.id.ivBookCover);
        ivOwnerAvatar = findViewById(R.id.ivOwnerAvatar);
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvCategory = findViewById(R.id.tvCategory);
        tvCondition = findViewById(R.id.tvCondition);
        tvDescription = findViewById(R.id.tvDescription);
        tvOwnerName = findViewById(R.id.tvOwnerName);
        layoutOwnerActions = findViewById(R.id.layoutOwnerActions);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadBookData() {
        bookViewModel.getBook(bookId).observe(this, new Observer<Book>() {
            @Override
            public void onChanged(Book book) {
                if (book != null) {
                    bindBookData(book);
                } else {
                    Toast.makeText(BookDetailActivity.this, "Không tìm thấy thông tin sách", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    private void bindBookData(Book book) {
        // Lưu dữ liệu tạm cho FR10
        currentBookTitle = book.getTitle() != null ? book.getTitle() : "Sách không tên";
        currentOwnerId = book.getOwnerId() != null ? book.getOwnerId() : "";

        tvTitle.setText(book.getTitle() != null ? book.getTitle() : "Chưa cập nhật");
        tvAuthor.setText(book.getAuthor() != null ? book.getAuthor() : "Chưa cập nhật");
        tvCategory.setText((book.getCategory() != null && !book.getCategory().isEmpty()) ? book.getCategory() : "Khác");
        tvCondition.setText((book.getCondition() != null && !book.getCondition().isEmpty()) ? book.getCondition() : "Mới");
        tvDescription.setText(book.getDescription() != null ? book.getDescription() : "Chưa có mô tả.");

        if (book.getImageUrl() != null && !book.getImageUrl().isEmpty()) {
            Glide.with(this).load(book.getImageUrl()).into(ivBookCover);
        } else {
            ivBookCover.setImageResource(R.drawable.ic_bookshare_logo);
        }

        // Tải thông tin chủ sách
        if (book.getOwnerId() != null && !book.getOwnerId().isEmpty()) {
            userRepository.getUserProfile(book.getOwnerId(), new UserRepository.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    tvOwnerName.setText(user.getFullName());
                    if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                        Glide.with(BookDetailActivity.this).load(user.getAvatarUrl()).into(ivOwnerAvatar);
                    }
                }
                @Override
                public void onError(String errorMessage) {
                    tvOwnerName.setText("Người đăng ẩn danh");
                }
            });
        }

        // Kiểm tra Owner Actions
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getUid().equals(book.getOwnerId())) {
            layoutOwnerActions.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bookViewModel.deleteBook(book.getId(), new BookRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(BookDetailActivity.this, "Đã xóa sách", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(BookDetailActivity.this, "Xóa lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(BookDetailActivity.this, "Chức năng Sửa sẽ được cập nhật", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            layoutOwnerActions.setVisibility(View.GONE);
        }
    }

    // ==========================================
    // PHẦN LOGIC CỦA TÍNH NĂNG BÌNH LUẬN (FR08)
    // ==========================================

    private void initRecyclerView() {
        commentAdapter = new CommentAdapter(new CommentAdapter.OnCommentActionListener() {
            @Override
            public void onEdit(Comment comment) {
                showCommentActionDialog(comment);
            }

            @Override
            public void onDelete(Comment comment) {
                confirmDeleteComment(comment);
            }
        });
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);
        rvComments.setNestedScrollingEnabled(false);
    }

    private void showCommentActionDialog(final Comment comment) {
        String[] options = {"Chỉnh sửa bình luận", "Xóa bình luận"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Tùy chọn bình luận")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            showEditCommentDialog(comment);
                        } else if (which == 1) {
                            confirmDeleteComment(comment);
                        }
                    }
                })
                .show();
    }

    private void showEditCommentDialog(final Comment comment) {
        final EditText input = new EditText(this);
        input.setText(comment.getContent());
        if (comment.getContent() != null) {
            input.setSelection(comment.getContent().length());
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Chỉnh sửa bình luận")
                .setView(input)
                .setPositiveButton("Lưu", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newText = input.getText().toString().trim();
                        if (!newText.isEmpty() && !newText.equals(comment.getContent())) {
                            commentViewModel.updateComment(bookId, comment.getCommentId(), newText);
                        }
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmDeleteComment(final Comment comment) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa bình luận")
                .setMessage("Bạn có chắc chắn muốn xóa bình luận này?")
                .setPositiveButton("Xóa", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        commentViewModel.deleteComment(bookId, comment.getCommentId());
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void observeViewModel() {
        if (bookId == null) return;

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
                    commentViewModel.resetStatus();
                }
            }
        });

        commentViewModel.getUpdateSuccess().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                if (success != null && success) {
                    Toast.makeText(BookDetailActivity.this, "Đã cập nhật bình luận!", Toast.LENGTH_SHORT).show();
                    commentViewModel.resetStatus();
                }
            }
        });

        commentViewModel.getDeleteSuccess().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                if (success != null && success) {
                    Toast.makeText(BookDetailActivity.this, "Đã xóa bình luận!", Toast.LENGTH_SHORT).show();
                    commentViewModel.resetStatus();
                }
            }
        });

        commentViewModel.getError().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String message) {
                if (message != null) {
                    btnSendComment.setEnabled(true);
                    Toast.makeText(BookDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    commentViewModel.resetStatus();
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

        // ==========================================
        // LẮNG NGHE YÊU CẦU SÁCH (FR10)
        // ==========================================
        requestViewModel.getActionSuccess().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                if (success != null && success) {
                    Toast.makeText(BookDetailActivity.this, "Đã gửi yêu cầu nhận sách thành công!", Toast.LENGTH_LONG).show();
                    requestViewModel.resetStatus();
                }
            }
        });

        requestViewModel.getError().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String message) {
                if (message != null) {
                    Toast.makeText(BookDetailActivity.this, message, Toast.LENGTH_LONG).show();
                    requestViewModel.resetStatus();
                }
            }
        });

        if (btnRequestBook != null) {
            btnRequestBook.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        Toast.makeText(BookDetailActivity.this, "Cần đăng nhập!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (currentOwnerId.equals(user.getUid())) {
                        Toast.makeText(BookDetailActivity.this, "Bạn không thể xin sách của chính mình!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String userName = user.getDisplayName() != null ? user.getDisplayName() : user.getEmail();
                    String userAvatar = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";

                    String coverUrl = bookViewModel.getBook(bookId).getValue() != null ? bookViewModel.getBook(bookId).getValue().getImageUrl() : "";
                    String message = "Mình rất muốn đọc cuốn này dịp cuối tuần, mình có thể qua lấy trực tiếp được ạ!";

                    String ownerNameText = tvOwnerName.getText().toString();

                    vn.edu.ueh.rtmiue20.project.data.model.Request newRequest = new vn.edu.ueh.rtmiue20.project.data.model.Request(
                            null,
                            bookId,
                            currentBookTitle,
                            coverUrl,
                            user.getUid(),
                            userName,
                            userAvatar,
                            currentOwnerId,
                            ownerNameText,
                            message,
                            vn.edu.ueh.rtmiue20.project.data.model.Request.STATUS_PENDING,
                            System.currentTimeMillis()
                    );

                    Toast.makeText(BookDetailActivity.this, "Đang gửi yêu cầu...", Toast.LENGTH_SHORT).show();
                    requestViewModel.sendRequest(newRequest);
                }
            });
        } // KẾT THÚC LOGIC FR10
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

    private boolean isOnline() {
        if (connectivityManager == null) return false;
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }
}