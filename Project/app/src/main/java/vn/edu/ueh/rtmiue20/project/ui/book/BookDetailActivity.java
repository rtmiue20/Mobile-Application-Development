package vn.edu.ueh.rtmiue20.project.ui.book;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.firebase.firestore.FirebaseFirestore;

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

    private String bookId;
    private BookViewModel bookViewModel;
    private UserRepository userRepository;

    // Ép kiểu View toàn bộ để dứt điểm lỗi ClassCastException với file XML
    private View btnBack, btnRequestBook, btnSendComment, btnFavorite;
    private View layoutOwnerActions, btnEdit, btnDelete;

    private ImageView ivBookCover, ivOwnerAvatar;
    private TextView tvTitle, tvAuthor, tvCategory, tvCondition, tvDescription, tvOwnerName;

    private RecyclerView rvComments;
    private EditText etComment;
    private CommentAdapter commentAdapter;
    private CommentViewModel commentViewModel;

    private FavoriteViewModel favoriteViewModel;

    private TextView tvOfflineBanner;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    private vn.edu.ueh.rtmiue20.project.ui.request.RequestViewModel requestViewModel;
    private String currentBookTitle = "Sách không tên";
    private String currentOwnerId = "";
    private String currentBookAuthor = "";
    private String currentBookCover = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_book_detail);

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
        if (bookId == null || bookId.trim().isEmpty() || bookId.contains("/")) {
            bookId = "mock_book_test_123";
        }

        initViews();

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isTaskRoot()) {
                    Intent homeIntent = new Intent(BookDetailActivity.this, vn.edu.ueh.rtmiue20.project.ui.home.HomeActivity.class);
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(homeIntent);
                }
                finish();
            }
        });

        bookViewModel = new ViewModelProvider(this).get(BookViewModel.class);
        userRepository = UserRepository.getInstance();
        commentViewModel = new ViewModelProvider(this).get(CommentViewModel.class);
        favoriteViewModel = new ViewModelProvider(this).get(FavoriteViewModel.class);
        requestViewModel = new ViewModelProvider(this).get(vn.edu.ueh.rtmiue20.project.ui.request.RequestViewModel.class);

        try {
            initRecyclerView();
            observeViewModel();
            setupListeners();
            observeFavorite();
            setupFavoriteListener();
            loadBookData();
        } catch (Exception e) {
            android.util.Log.e("BookDetailActivity", "Lỗi khởi tạo components: " + e.getMessage(), e);
        }

        tvOfflineBanner = findViewById(R.id.tvOfflineBanner);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (tvOfflineBanner != null) {
            tvOfflineBanner.setVisibility(isOnline() ? View.GONE : View.VISIBLE);
        }

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(() -> {
                    if (tvOfflineBanner != null) tvOfflineBanner.setVisibility(View.GONE);
                });
            }
            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> {
                    if (tvOfflineBanner != null) tvOfflineBanner.setVisibility(View.VISIBLE);
                });
            }
        };

        try {
            if (connectivityManager != null) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            }
        } catch (Exception e) {
            // Chặn lỗi SecurityException
        }
    }

    private void initViews() {
        ivBookCover = findViewById(R.id.ivBookCover);
        ivOwnerAvatar = findViewById(R.id.ivOwnerAvatar);

        btnBack = findViewById(R.id.btnBack);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnRequestBook = findViewById(R.id.btn_request_book);
        btnFavorite = findViewById(R.id.btn_favorite);
        btnSendComment = findViewById(R.id.btn_send_comment);
        layoutOwnerActions = findViewById(R.id.layoutOwnerActions);

        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvCategory = findViewById(R.id.tvCategory);
        tvCondition = findViewById(R.id.tvCondition);
        tvDescription = findViewById(R.id.tvDescription);
        tvOwnerName = findViewById(R.id.tvOwnerName);

        rvComments = findViewById(R.id.rvComments);
        etComment = findViewById(R.id.et_comment);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void loadBookData() {
        bookViewModel.getBook(bookId).observe(this, new Observer<Book>() {
            @Override
            public void onChanged(Book book) {
                if (book != null) {
                    bindBookData(book);
                } else {
                    // Dự phòng tải trực tiếp từ Firestore nếu local cache Room chưa đồng bộ
                    fetchBookFromFirestore(bookId);
                }
            }
        });
    }

    private void fetchBookFromFirestore(String id) {
        if (id == null || id.trim().isEmpty() || id.equals("mock_book_test_123")) return;
        FirebaseFirestore.getInstance().collection("books").document(id).get()
                .addOnSuccessListener(doc -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (doc != null && doc.exists()) {
                        try {
                            Book b = doc.toObject(Book.class);
                            if (b != null) {
                                b.setId(doc.getId());
                                bindBookData(b);
                            }
                        } catch (Exception e) {
                            android.util.Log.e("BookDetailActivity", "Lỗi parse Book từ Firestore: " + e.getMessage());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("BookDetailActivity", "Lỗi fetch Book từ Firestore: " + e.getMessage());
                });
    }

    private void bindBookData(Book book) {
        if (book == null) return;
        try {
            currentBookTitle = book.getTitle() != null ? book.getTitle() : "Sách không tên";
            currentOwnerId = book.getOwnerId() != null ? book.getOwnerId() : "";
            currentBookAuthor = book.getAuthor() != null ? book.getAuthor() : "";
            currentBookCover = book.getImageUrl() != null ? book.getImageUrl() : "";

            if (tvTitle != null) tvTitle.setText(currentBookTitle);
            if (tvAuthor != null) tvAuthor.setText(!currentBookAuthor.isEmpty() ? currentBookAuthor : "Chưa cập nhật");
            if (tvCategory != null) tvCategory.setText((book.getCategory() != null && !book.getCategory().isEmpty()) ? book.getCategory() : "Khác");
            if (tvCondition != null) tvCondition.setText((book.getCondition() != null && !book.getCondition().isEmpty()) ? book.getCondition() : "Mới");
            if (tvDescription != null) tvDescription.setText(book.getDescription() != null ? book.getDescription() : "Chưa có mô tả.");

            if (ivBookCover != null) {
                if (book.getImageUrl() != null && !book.getImageUrl().isEmpty()) {
                    Glide.with(this).load(book.getImageUrl()).into(ivBookCover);
                } else {
                    ivBookCover.setImageResource(R.drawable.ic_bookshare_logo);
                }
            }

            if (book.getOwnerId() != null && !book.getOwnerId().isEmpty()) {
                userRepository.getUserProfile(book.getOwnerId(), new UserRepository.UserCallback() {
                    @Override
                    public void onSuccess(User user) {
                        if (isFinishing() || isDestroyed()) return;
                        String name = user.getFullName();
                        if (name == null || name.trim().isEmpty()) name = user.getNickname();
                        if (name == null || name.trim().isEmpty()) name = user.getEmail();
                        if (name == null || name.trim().isEmpty()) name = "Người đăng ẩn danh";
                        if (tvOwnerName != null) tvOwnerName.setText(name);
                        if (ivOwnerAvatar != null && user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                            Glide.with(BookDetailActivity.this).load(user.getAvatarUrl()).into(ivOwnerAvatar);
                        }
                    }
                    @Override
                    public void onError(String errorMessage) {
                        if (isFinishing() || isDestroyed()) return;
                        if (tvOwnerName != null) tvOwnerName.setText("Người đăng ẩn danh");
                    }
                });
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null && currentUser.getUid().equals(book.getOwnerId())) {
                if (layoutOwnerActions != null) layoutOwnerActions.setVisibility(View.VISIBLE);

                if (btnDelete != null) {
                    btnDelete.setOnClickListener(v -> {
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
                    });
                }
                if (btnEdit != null) {
                    btnEdit.setOnClickListener(v -> Toast.makeText(BookDetailActivity.this, "Chức năng Sửa sẽ được cập nhật", Toast.LENGTH_SHORT).show());
                }
            } else {
                if (layoutOwnerActions != null) layoutOwnerActions.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            android.util.Log.e("BookDetailActivity", "Lỗi bind dữ liệu sách: " + e.getMessage(), e);
        }
    }

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
        if (rvComments != null) {
            rvComments.setLayoutManager(new LinearLayoutManager(this));
            rvComments.setAdapter(commentAdapter);
            rvComments.setNestedScrollingEnabled(false);
        }
    }

    private void showCommentActionDialog(final Comment comment) {
        String[] options = {"Chỉnh sửa bình luận", "Xóa bình luận"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Tùy chọn bình luận")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showEditCommentDialog(comment);
                    else if (which == 1) confirmDeleteComment(comment);
                }).show();
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
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newText = input.getText().toString().trim();
                    if (!newText.isEmpty() && !newText.equals(comment.getContent())) {
                        commentViewModel.updateComment(bookId, comment.getCommentId(), newText);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmDeleteComment(final Comment comment) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa bình luận")
                .setMessage("Bạn có chắc chắn muốn xóa bình luận này?")
                .setPositiveButton("Xóa", (dialog, which) -> commentViewModel.deleteComment(bookId, comment.getCommentId()))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void observeViewModel() {
        if (bookId == null) return;

        commentViewModel.getComments(bookId).observe(this, comments -> {
            if (commentAdapter != null) commentAdapter.setComments(comments);
        });

        commentViewModel.getAddSuccess().observe(this, success -> {
            if (success != null && success) {
                if (etComment != null) etComment.setText("");
                if (btnSendComment != null) btnSendComment.setEnabled(true);
                commentViewModel.resetStatus();
            }
        });

        commentViewModel.getUpdateSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(BookDetailActivity.this, "Đã cập nhật bình luận!", Toast.LENGTH_SHORT).show();
                commentViewModel.resetStatus();
            }
        });

        commentViewModel.getDeleteSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(BookDetailActivity.this, "Đã xóa bình luận!", Toast.LENGTH_SHORT).show();
                commentViewModel.resetStatus();
            }
        });

        commentViewModel.getError().observe(this, message -> {
            if (message != null) {
                if (btnSendComment != null) btnSendComment.setEnabled(true);
                Toast.makeText(BookDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                commentViewModel.resetStatus();
            }
        });
    }

    private void setupListeners() {
        if (btnSendComment != null) {
            btnSendComment.setOnClickListener(v -> {
                if (etComment == null) return;
                String content = etComment.getText().toString().trim();
                if (content.isEmpty()) {
                    Toast.makeText(BookDetailActivity.this, getString(R.string.msg_comment_empty), Toast.LENGTH_SHORT).show();
                    return;
                }
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                    Toast.makeText(BookDetailActivity.this, getString(R.string.msg_comment_need_login), Toast.LENGTH_SHORT).show();
                    return;
                }
                String userName = user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty() ? user.getDisplayName() : user.getEmail();
                Comment comment = new Comment(null, user.getUid(), userName, content, System.currentTimeMillis());
                btnSendComment.setEnabled(false);
                commentViewModel.addComment(bookId, comment);
            });
        }

        requestViewModel.getActionSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(BookDetailActivity.this, "Đã gửi yêu cầu nhận sách thành công!", Toast.LENGTH_LONG).show();
                requestViewModel.resetStatus();
            }
        });

        requestViewModel.getError().observe(this, message -> {
            if (message != null) {
                Toast.makeText(BookDetailActivity.this, message, Toast.LENGTH_LONG).show();
                requestViewModel.resetStatus();
            }
        });

        if (btnRequestBook != null) {
            btnRequestBook.setOnClickListener(v -> {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                    Toast.makeText(BookDetailActivity.this, "Cần đăng nhập!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (currentOwnerId.equals(user.getUid())) {
                    Toast.makeText(BookDetailActivity.this, "Bạn không thể xin sách của chính mình!", Toast.LENGTH_SHORT).show();
                    return;
                }
                String userName = user.getDisplayName() != null && !user.getDisplayName().isEmpty() ? user.getDisplayName() : user.getEmail();
                String userAvatar = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";
                String coverUrl = !currentBookCover.isEmpty() ? currentBookCover :
                        (bookViewModel.getBook(bookId).getValue() != null ? bookViewModel.getBook(bookId).getValue().getImageUrl() : "");
                String ownerNameText = tvOwnerName != null ? tvOwnerName.getText().toString() : "Chủ sách";

                vn.edu.ueh.rtmiue20.project.data.model.Request newRequest = new vn.edu.ueh.rtmiue20.project.data.model.Request(
                        null, bookId, currentBookTitle, coverUrl, user.getUid(), userName, userAvatar,
                        currentOwnerId, ownerNameText, "Mình rất muốn đọc cuốn này dịp cuối tuần!",
                        vn.edu.ueh.rtmiue20.project.data.model.Request.STATUS_PENDING, System.currentTimeMillis()
                );
                if (currentBookAuthor != null && !currentBookAuthor.isEmpty()) {
                    newRequest.setBookAuthor(currentBookAuthor);
                }
                Toast.makeText(BookDetailActivity.this, "Đang gửi yêu cầu trao đổi...", Toast.LENGTH_SHORT).show();
                requestViewModel.sendRequest(newRequest);
            });
        }
    }

    private void observeFavorite() {
        if (bookId == null) return;
        favoriteViewModel.getIsFavorited(bookId).observe(this, isFavorited -> {
            if (btnFavorite instanceof ImageView) {
                ImageView ivFav = (ImageView) btnFavorite;
                if (isFavorited != null && isFavorited) {
                    ivFav.setImageResource(R.drawable.ic_heart_filled);
                    ivFav.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.red_danger));
                } else {
                    ivFav.setImageResource(R.drawable.ic_heart_outline);
                    ivFav.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary));
                }
            }
        });
        favoriteViewModel.getError().observe(this, message -> {
            if (message != null) {
                Toast.makeText(BookDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                favoriteViewModel.resetError();
            }
        });
    }

    private void setupFavoriteListener() {
        if (btnFavorite != null) {
            btnFavorite.setOnClickListener(v -> {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                    Toast.makeText(BookDetailActivity.this, "Vui lòng đăng nhập để lưu bài yêu thích", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (bookId != null && !bookId.trim().isEmpty()) {
                    boolean currentFav = Boolean.TRUE.equals(favoriteViewModel.getIsFavorited(bookId).getValue());
                    Toast.makeText(BookDetailActivity.this,
                            currentFav ? "Đã xóa khỏi mục Yêu thích" : "Đã thêm vào mục Yêu thích",
                            Toast.LENGTH_SHORT).show();
                    favoriteViewModel.toggleFavorite(bookId);
                }
            });
        }
    }

    private boolean isOnline() {
        try {
            if (connectivityManager == null) return false;
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (connectivityManager != null && networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        } catch (Exception e) {
            // Chặn lỗi
        }
    }
}