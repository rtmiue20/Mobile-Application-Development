package vn.edu.ueh.rtmiue20.project.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import vn.edu.ueh.rtmiue20.project.MainActivity;
import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.Book;
import vn.edu.ueh.rtmiue20.project.data.model.Request;
import vn.edu.ueh.rtmiue20.project.data.repository.AuthRepository;
import vn.edu.ueh.rtmiue20.project.data.repository.BookRepository;
import vn.edu.ueh.rtmiue20.project.ui.book.BookDetailActivity;
import vn.edu.ueh.rtmiue20.project.ui.home.BookAdapter;
import vn.edu.ueh.rtmiue20.project.ui.home.BookViewModel;
import vn.edu.ueh.rtmiue20.project.ui.home.HomeActivity;
import vn.edu.ueh.rtmiue20.project.util.ViewPressEffectHelper;

public class AdminDashboardActivity extends AppCompatActivity {

    private ImageView btnBack;
    private ImageView btnLog;
    private TextView btnCheckNow;
    private View btnLogoutAdmin;
    private View btnViewMemberFeed;
    private ScrollView adminScrollView;
    private View sectionManageBooks;

    private TextView tvAdminTotalUsers;
    private TextView tvAdminTotalBooks;
    private TextView tvAdminBooksSub;
    private TextView tvAdminPendingRequests;
    private TextView tvAdminRequestsSub;
    private TextView tvAdminFlaggedCount;
    private TextView tvAdminRecentCountBadge;
    private TextView tvAdminLogSummary;
    private TextView tvAdminInteractionCount;
    private TextView tvCommunityHealthPercent;
    private ProgressBar pbCommunityHealth;
    private TextView tvAdminEmptyBooks;
    private RecyclerView rvAdminBooks;

    private AuthRepository authRepository;
    private BookViewModel bookViewModel;
    private BookAdapter adminBookAdapter;

    private int totalUsersCount = 0;
    private int bannedUsersCount = 0;
    private int hiddenBooksCount = 0;
    private int totalBooksCount = 0;
    private int totalRequestsCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_dashboard);

        View mainView = findViewById(R.id.mainAdminDashboard);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        authRepository = AuthRepository.getInstance();
        bookViewModel = new ViewModelProvider(this).get(BookViewModel.class);

        initViews();
        setupRecyclerView();
        setupListeners();
        observeRealTimeMetrics();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnLog = findViewById(R.id.btnLog);
        btnCheckNow = findViewById(R.id.btnCheckNow);
        btnLogoutAdmin = findViewById(R.id.btnLogoutAdmin);
        btnViewMemberFeed = findViewById(R.id.btnViewMemberFeed);
        adminScrollView = findViewById(R.id.adminScrollView);
        sectionManageBooks = findViewById(R.id.sectionManageBooks);

        tvAdminTotalUsers = findViewById(R.id.tvAdminTotalUsers);
        tvAdminTotalBooks = findViewById(R.id.tvAdminTotalBooks);
        tvAdminBooksSub = findViewById(R.id.tvAdminBooksSub);
        tvAdminPendingRequests = findViewById(R.id.tvAdminPendingRequests);
        tvAdminRequestsSub = findViewById(R.id.tvAdminRequestsSub);
        tvAdminFlaggedCount = findViewById(R.id.tvAdminFlaggedCount);
        tvAdminRecentCountBadge = findViewById(R.id.tvAdminRecentCountBadge);
        tvAdminLogSummary = findViewById(R.id.tvAdminLogSummary);
        tvAdminInteractionCount = findViewById(R.id.tvAdminInteractionCount);
        tvCommunityHealthPercent = findViewById(R.id.tvCommunityHealthPercent);
        pbCommunityHealth = findViewById(R.id.pbCommunityHealth);
        tvAdminEmptyBooks = findViewById(R.id.tvAdminEmptyBooks);
        rvAdminBooks = findViewById(R.id.rvAdminBooks);
    }

    private void setupRecyclerView() {
        if (rvAdminBooks == null) return;
        int columns = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 4 : 2;
        rvAdminBooks.setLayoutManager(new GridLayoutManager(this, columns));
        adminBookAdapter = new BookAdapter(this::showAdminBookModerationDialog);
        rvAdminBooks.setAdapter(adminBookAdapter);
    }

    private void showAdminBookModerationDialog(Book book) {
        if (book == null) return;
        boolean isHidden = "hidden".equalsIgnoreCase(book.getStatus());
        String toggleHideLabel = isHidden ? "Bỏ ẩn bài đăng (Hiển thị lại)" : "Ẩn bài đăng (Vi phạm / Spam)";

        String[] options = new String[]{
                "Xem chi tiết bài đăng",
                toggleHideLabel,
                "Xóa vĩnh viễn bài đăng"
        };

        new AlertDialog.Builder(this)
                .setTitle("Kiểm duyệt: " + (book.getTitle() != null ? book.getTitle() : "Bài đăng sách"))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(AdminDashboardActivity.this, BookDetailActivity.class);
                        intent.putExtra("bookId", book.getId());
                        startActivity(intent);
                    } else if (which == 1) {
                        book.setStatus(isHidden ? "available" : "hidden");
                        bookViewModel.saveBook(book, new BookRepository.ActionCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(AdminDashboardActivity.this,
                                        isHidden ? "Đã khôi phục hiển thị bài đăng!" : "Đã ẩn bài đăng khỏi trang chủ!",
                                        Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(AdminDashboardActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else if (which == 2) {
                        new AlertDialog.Builder(AdminDashboardActivity.this)
                                .setTitle("Xác nhận xóa bài đăng")
                                .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn sách \"" + book.getTitle() + "\" khỏi hệ thống?")
                                .setPositiveButton("Xóa vĩnh viễn", (d, w) -> {
                                    bookViewModel.deleteBook(book.getId(), new BookRepository.ActionCallback() {
                                        @Override
                                        public void onSuccess() {
                                            Toast.makeText(AdminDashboardActivity.this, "Đã xóa bài đăng thành công!", Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onError(String errorMessage) {
                                            Toast.makeText(AdminDashboardActivity.this, "Lỗi xóa: " + errorMessage, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                })
                                .setNegativeButton("Hủy", null)
                                .show();
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void observeRealTimeMetrics() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Thống kê Thành viên thực tế từ Firestore
        db.collection("users").addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;
            totalUsersCount = snap.size();
            int banned = 0;
            for (DocumentSnapshot d : snap.getDocuments()) {
                if ("banned".equalsIgnoreCase(d.getString("status"))) {
                    banned++;
                }
            }
            bannedUsersCount = banned;
            if (tvAdminTotalUsers != null) {
                tvAdminTotalUsers.setText(String.valueOf(totalUsersCount));
            }
            updateFlaggedAndLogSummary();
        });

        // 2. Thống kê Sách cộng đồng thực tế (qua BookViewModel đồng bộ Room & Firestore)
        bookViewModel.getAllBooks().observe(this, books -> {
            if (books == null) return;
            totalBooksCount = books.size();
            int visibleCount = 0;
            int hiddenCount = 0;
            long totalViewsAndFavs = 0;

            for (Book b : books) {
                if ("hidden".equalsIgnoreCase(b.getStatus())) {
                    hiddenCount++;
                } else {
                    visibleCount++;
                }
                totalViewsAndFavs += Math.max(0, b.getViewCount()) + Math.max(0, b.getFavoriteCount());
            }
            hiddenBooksCount = hiddenCount;

            if (tvAdminTotalBooks != null) {
                tvAdminTotalBooks.setText(String.valueOf(totalBooksCount));
            }
            if (tvAdminBooksSub != null) {
                tvAdminBooksSub.setText(visibleCount + " đang hiển thị");
            }
            if (tvAdminRecentCountBadge != null) {
                tvAdminRecentCountBadge.setText(String.valueOf(totalBooksCount));
            }
            if (btnCheckNow != null) {
                btnCheckNow.setText("Quản lý tin (" + totalBooksCount + ")");
            }

            if (adminBookAdapter != null) {
                List<Book> previewList = books.size() > 8 ? books.subList(0, 8) : books;
                adminBookAdapter.update(previewList);
            }
            if (tvAdminEmptyBooks != null) {
                tvAdminEmptyBooks.setVisibility(books.isEmpty() ? View.VISIBLE : View.GONE);
            }

            if (tvAdminInteractionCount != null) {
                tvAdminInteractionCount.setText("🤝 " + (totalViewsAndFavs + totalRequestsCount) + " Lượt tương tác");
            }

            updateFlaggedAndLogSummary();
        });

        // 3. Thống kê Yêu cầu giao dịch sách thực tế từ Firestore
        db.collection("requests").addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;
            totalRequestsCount = snap.size();
            int pending = 0;
            int accepted = 0;
            for (DocumentSnapshot d : snap.getDocuments()) {
                String st = d.getString("status");
                if (Request.STATUS_PENDING.equalsIgnoreCase(st)) {
                    pending++;
                } else if (Request.STATUS_ACCEPTED.equalsIgnoreCase(st)) {
                    accepted++;
                }
            }
            if (tvAdminPendingRequests != null) {
                tvAdminPendingRequests.setText(String.valueOf(totalRequestsCount));
            }
            if (tvAdminRequestsSub != null) {
                tvAdminRequestsSub.setText(pending + " chờ duyệt • " + accepted + " hoàn tất");
            }
            updateFlaggedAndLogSummary();
        });
    }

    private void updateFlaggedAndLogSummary() {
        int totalFlagged = bannedUsersCount + hiddenBooksCount;
        if (tvAdminFlaggedCount != null) {
            tvAdminFlaggedCount.setText(String.valueOf(totalFlagged));
        }
        if (tvAdminLogSummary != null) {
            tvAdminLogSummary.setText(totalUsersCount + " thành viên • " + totalBooksCount + " sách • " + totalRequestsCount + " giao dịch");
        }
        int safePercent = totalBooksCount > 0
                ? Math.max(0, Math.min(100, (int) Math.round(((totalBooksCount - hiddenBooksCount) * 100.0) / totalBooksCount)))
                : 100;
        if (tvCommunityHealthPercent != null) {
            tvCommunityHealthPercent.setText(safePercent + "% Hoạt động tốt");
        }
        if (pbCommunityHealth != null) {
            pbCommunityHealth.setProgress(safePercent);
        }
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnLogoutAdmin != null) {
            btnLogoutAdmin.setOnClickListener(v -> handleLogout());
        }

        if (btnLog != null) {
            btnLog.setOnClickListener(v -> {
                String summary = tvAdminLogSummary != null ? tvAdminLogSummary.getText().toString() : "";
                Toast.makeText(this, "Thống kê hệ thống: " + summary, Toast.LENGTH_SHORT).show();
            });
        }

        View.OnClickListener scrollToBooksListener = v -> {
            if (adminScrollView != null && sectionManageBooks != null) {
                adminScrollView.smoothScrollTo(0, sectionManageBooks.getTop());
            }
        };

        if (btnCheckNow != null) {
            btnCheckNow.setOnClickListener(scrollToBooksListener);
        }

        View cardTotalBooks = findViewById(R.id.cardTotalBooks);
        if (cardTotalBooks != null) {
            cardTotalBooks.setOnClickListener(scrollToBooksListener);
        }

        View cardTotalMembers = findViewById(R.id.cardTotalMembers);
        if (cardTotalMembers != null) {
            cardTotalMembers.setOnClickListener(v ->
                    startActivity(new Intent(AdminDashboardActivity.this, UserManagementActivity.class))
            );
        }

        View btnManageMembers = findViewById(R.id.btnManageMembers);
        if (btnManageMembers != null) {
            btnManageMembers.setOnClickListener(v ->
                    startActivity(new Intent(AdminDashboardActivity.this, UserManagementActivity.class))
            );
        }

        if (btnViewMemberFeed != null) {
            btnViewMemberFeed.setOnClickListener(v ->
                    startActivity(new Intent(AdminDashboardActivity.this, HomeActivity.class))
            );
        }

        ViewPressEffectHelper.applyPressEffect(
                btnLogoutAdmin, btnBack, cardTotalMembers, btnManageMembers, btnCheckNow, btnViewMemberFeed
        );
    }

    private void handleLogout() {
        authRepository.logout();
        Toast.makeText(this, "Đã đăng xuất khỏi Trang Quản Trị.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(AdminDashboardActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
