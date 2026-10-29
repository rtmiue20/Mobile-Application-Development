package vn.edu.ueh.rtmiue20.project.ui.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.rtmiue20.project.MainActivity;
import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.Book;
import vn.edu.ueh.rtmiue20.project.data.model.Request;
import vn.edu.ueh.rtmiue20.project.data.model.User;
import vn.edu.ueh.rtmiue20.project.data.repository.AuthRepository;
import vn.edu.ueh.rtmiue20.project.data.repository.BookRepository;
import vn.edu.ueh.rtmiue20.project.data.repository.RequestRepository;
import vn.edu.ueh.rtmiue20.project.data.repository.UserRepository;
import vn.edu.ueh.rtmiue20.project.ui.book.BookDetailActivity;
import vn.edu.ueh.rtmiue20.project.ui.home.BookAdapter;
import vn.edu.ueh.rtmiue20.project.ui.home.BookViewModel;
import vn.edu.ueh.rtmiue20.project.ui.home.HomeActivity;
import vn.edu.ueh.rtmiue20.project.ui.profile.ProfileActivity;
import vn.edu.ueh.rtmiue20.project.ui.request.ManageRequestsActivity;
import vn.edu.ueh.rtmiue20.project.util.ImageHelper;
import vn.edu.ueh.rtmiue20.project.util.ViewPressEffectHelper;

public class AdminDashboardActivity extends AppCompatActivity {

    private ImageView btnLog;
    private ImageView ivAdminAvatar;
    private TextView btnCheckNow;
    private View btnLogoutAdmin;
    private View btnViewMemberFeed;
    private View cardSystemLog;
    private View cardTotalRequests;
    private View cardFlaggedItems;
    private TextView tvAdminViewAllBooks;
    private TextView tvReviewTitle;
    private androidx.core.widget.NestedScrollView adminScrollView;
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

    // Pagination for Books
    private static final int BOOKS_PAGE_SIZE = 4;
    private int currentBookPage = 1;
    private final List<Book> currentFilteredBooks = new ArrayList<>();
    private LinearLayout layoutBookPagination;
    private TextView btnPrevPage;
    private TextView btnNextPage;
    private TextView tvPageIndicator;

    private AuthRepository authRepository;
    private BookViewModel bookViewModel;
    private BookAdapter adminBookAdapter;
    private List<Book> allSystemBooks = new ArrayList<>();
    private String currentBookFilterMode = "all"; // all, visible, hidden

    private int totalUsersCount = 0;
    private int bannedUsersCount = 0;
    private int hiddenBooksCount = 0;
    private int pendingBooksCount = 0;
    private int totalBooksCount = 0;
    private int totalRequestsCount = 0;
    private int pendingRequestsCount = 0;
    private int acceptedRequestsCount = 0;
    private int totalFavoritesCount = 0;

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

        getSharedPreferences("BookSharePrefs", MODE_PRIVATE)
                .edit()
                .putString("user_role", "admin")
                .apply();

        authRepository = AuthRepository.getInstance();
        bookViewModel = new ViewModelProvider(this).get(BookViewModel.class);

        initViews();
        setupRecyclerView();
        setupListeners();
        observeRealTimeMetrics();
        loadAdminAvatar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAdminAvatar();
    }

    private void loadAdminAvatar() {
        if (ivAdminAvatar == null) return;
        SharedPreferences prefs = getSharedPreferences("BookSharePrefs", MODE_PRIVATE);
        if (prefs.contains("user_avatar_url")) {
            String cachedAvatar = prefs.getString("user_avatar_url", "");
            ImageHelper.bindAdminHeaderAvatar(this, ivAdminAvatar, cachedAvatar);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            UserRepository.getInstance().getUserProfile(user.getUid(), new UserRepository.UserCallback() {
                @Override
                public void onSuccess(User userProfile) {
                    if (isFinishing() || isDestroyed()) return;
                    String avatarUrl = (userProfile != null && userProfile.getAvatarUrl() != null)
                            ? userProfile.getAvatarUrl()
                            : "";
                    prefs.edit()
                            .putString("user_avatar_url", avatarUrl)
                            .putString("user_role", "admin")
                            .apply();
                    ImageHelper.bindAdminHeaderAvatar(AdminDashboardActivity.this, ivAdminAvatar, avatarUrl);
                }

                @Override
                public void onError(String errorMessage) {}
            });
        }
    }

    private void initViews() {
        btnLog = findViewById(R.id.btnLog);
        ivAdminAvatar = findViewById(R.id.ivAdminAvatar);
        btnCheckNow = findViewById(R.id.btnCheckNow);
        btnLogoutAdmin = findViewById(R.id.btnLogoutAdmin);
        btnViewMemberFeed = findViewById(R.id.btnViewMemberFeed);
        cardSystemLog = findViewById(R.id.cardSystemLog);
        cardTotalRequests = findViewById(R.id.cardTotalRequests);
        cardFlaggedItems = findViewById(R.id.cardFlaggedItems);
        tvAdminViewAllBooks = findViewById(R.id.tvAdminViewAllBooks);
        tvReviewTitle = findViewById(R.id.tvReviewTitle);
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
        layoutBookPagination = findViewById(R.id.layoutBookPagination);
        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        tvPageIndicator = findViewById(R.id.tvPageIndicator);
    }

    private void setupRecyclerView() {
        if (rvAdminBooks == null) return;
        int columns = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 4 : 2;
        rvAdminBooks.setLayoutManager(new GridLayoutManager(this, columns));
        adminBookAdapter = new BookAdapter(this::showAdminBookModerationDialog);
        rvAdminBooks.setAdapter(adminBookAdapter);
    }

    private void applyAdminBookFilter() {
        if (adminBookAdapter == null) return;
        currentFilteredBooks.clear();
        for (Book b : allSystemBooks) {
            boolean isHidden = "hidden".equalsIgnoreCase(b.getStatus());
            boolean isPending = Book.isPendingReview(b);
            if ("visible".equals(currentBookFilterMode) && (isHidden || isPending)) continue;
            if ("hidden".equals(currentBookFilterMode) && !isHidden) continue;
            if ("pending".equals(currentBookFilterMode) && !isPending) continue;
            currentFilteredBooks.add(b);
        }
        // TC-ADMIN-06: Đưa sách "Chờ duyệt" lên đầu để Admin xử lý trước
        java.util.Collections.sort(currentFilteredBooks, (a, b) ->
                Boolean.compare(Book.isPendingReview(b), Book.isPendingReview(a)));

        int totalItems = currentFilteredBooks.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / BOOKS_PAGE_SIZE));
        if (currentBookPage > totalPages) {
            currentBookPage = totalPages;
        }
        if (currentBookPage < 1) {
            currentBookPage = 1;
        }

        displayCurrentBookPage(false);

        if (tvReviewTitle != null) {
            if ("hidden".equals(currentBookFilterMode)) {
                tvReviewTitle.setText("Bài đăng đã ẩn");
            } else if ("visible".equals(currentBookFilterMode)) {
                tvReviewTitle.setText("Bài đăng đang hiển thị");
            } else if ("pending".equals(currentBookFilterMode)) {
                tvReviewTitle.setText("Bài đăng chờ duyệt");
            } else {
                tvReviewTitle.setText("Quản lý bài đăng sách");
            }
        }
        if (tvAdminRecentCountBadge != null) {
            tvAdminRecentCountBadge.setText(String.valueOf(totalItems));
        }
        if (tvAdminEmptyBooks != null) {
            if (currentFilteredBooks.isEmpty()) {
                tvAdminEmptyBooks.setText("hidden".equals(currentBookFilterMode)
                        ? "Không có bài đăng nào đang bị ẩn."
                        : "pending".equals(currentBookFilterMode)
                        ? "Không có bài đăng nào đang chờ duyệt."
                        : "Chưa có bài đăng sách nào trên hệ thống.");
                tvAdminEmptyBooks.setVisibility(View.VISIBLE);
            } else {
                tvAdminEmptyBooks.setVisibility(View.GONE);
            }
        }
    }

    private void displayCurrentBookPage(boolean shouldScroll) {
        if (adminBookAdapter == null) return;
        int totalItems = currentFilteredBooks.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / BOOKS_PAGE_SIZE));

        if (currentBookPage > totalPages) currentBookPage = totalPages;
        if (currentBookPage < 1) currentBookPage = 1;

        if (totalItems == 0) {
            adminBookAdapter.update(new ArrayList<>());
            if (layoutBookPagination != null) {
                layoutBookPagination.setVisibility(View.GONE);
            }
            return;
        }

        int fromIndex = (currentBookPage - 1) * BOOKS_PAGE_SIZE;
        int toIndex = Math.min(fromIndex + BOOKS_PAGE_SIZE, totalItems);
        List<Book> pageBooks = new ArrayList<>(currentFilteredBooks.subList(fromIndex, toIndex));
        adminBookAdapter.update(pageBooks);

        if (rvAdminBooks != null) {
            rvAdminBooks.post(rvAdminBooks::requestLayout);
        }

        if (layoutBookPagination != null) {
            if (totalItems <= BOOKS_PAGE_SIZE) {
                layoutBookPagination.setVisibility(View.GONE);
            } else {
                layoutBookPagination.setVisibility(View.VISIBLE);

                if (tvPageIndicator != null) {
                    tvPageIndicator.setText("Trang " + currentBookPage + " / " + totalPages + " (" + totalItems + " sách)");
                }

                boolean hasPrev = currentBookPage > 1;
                boolean hasNext = currentBookPage < totalPages;

                if (btnPrevPage != null) {
                    btnPrevPage.setEnabled(hasPrev);
                    btnPrevPage.setAlpha(hasPrev ? 1.0f : 0.35f);
                }
                if (btnNextPage != null) {
                    btnNextPage.setEnabled(hasNext);
                    btnNextPage.setAlpha(hasNext ? 1.0f : 0.35f);
                }
            }
        }

        if (shouldScroll && adminScrollView != null && sectionManageBooks != null) {
            adminScrollView.post(() -> adminScrollView.smoothScrollTo(0, sectionManageBooks.getTop()));
        }
    }

    private void showPagePickerDialog() {
        int totalItems = currentFilteredBooks.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / BOOKS_PAGE_SIZE));
        if (totalPages <= 1) return;

        String[] pages = new String[totalPages];
        for (int i = 0; i < totalPages; i++) {
            int start = i * BOOKS_PAGE_SIZE + 1;
            int end = Math.min((i + 1) * BOOKS_PAGE_SIZE, totalItems);
            pages[i] = "Trang " + (i + 1) + " (Sách " + start + " - " + end + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("Chọn trang bài đăng sách")
                .setSingleChoiceItems(pages, currentBookPage - 1, (dialog, which) -> {
                    currentBookPage = which + 1;
                    displayCurrentBookPage(true);
                    dialog.dismiss();
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void showAdminBookModerationDialog(Book book) {
        if (book == null) return;
        String[] options = new String[]{
                "Xem chi tiết bài đăng",
                "Phê duyệt bài đăng (Hiển thị công khai trên Trang chủ)",
                "Từ chối / Ẩn bài đăng vi phạm (Ẩn khỏi Trang chủ)",
                "Xóa vĩnh viễn bài đăng"
        };
        String stateLabel = Book.isPendingReview(book) ? "⏳ Chờ duyệt"
                : "hidden".equalsIgnoreCase(book.getStatus()) ? "🚫 Đã ẩn" : "✅ Đang hiển thị";

        new AlertDialog.Builder(this)
                .setTitle("Kiểm duyệt: " + (book.getTitle() != null ? book.getTitle() : "Bài đăng sách") + " (" + stateLabel + ")")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(AdminDashboardActivity.this, BookDetailActivity.class);
                        intent.putExtra("bookId", book.getId());
                        startActivity(intent);
                    } else if (which == 1) {
                        // Khôi phục đúng hình thức Member đã chọn: "available" (Miễn phí) / "exchanging" (Đang trao đổi)
                        final String approvedStatus = Book.resolveApprovedStatus(book);
                        book.setStatus(approvedStatus);
                        bookViewModel.saveBook(book, new BookRepository.ActionCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(AdminDashboardActivity.this,
                                        "Đã phê duyệt! Sách hiển thị công khai với nhãn \""
                                                + (Book.STATUS_EXCHANGING.equals(approvedStatus) ? "Đang trao đổi" : "Miễn phí") + "\".",
                                        Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(AdminDashboardActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else if (which == 2) {
                        book.setStatus("hidden");
                        bookViewModel.saveBook(book, new BookRepository.ActionCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(AdminDashboardActivity.this,
                                        "Đã ẩn bài đăng vi phạm khỏi Trang chủ!",
                                        Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(AdminDashboardActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else if (which == 3) {
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
                String st = d.getString("status");
                if ("banned".equalsIgnoreCase(st) || "locked".equalsIgnoreCase(st)) {
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
            allSystemBooks = new ArrayList<>(books);
            totalBooksCount = books.size();
            int visibleCount = 0;
            int hiddenCount = 0;
            int pendingCount = 0;
            long totalViewsAndFavs = 0;

            for (Book b : books) {
                if ("hidden".equalsIgnoreCase(b.getStatus())) {
                    hiddenCount++;
                } else if (Book.isPendingReview(b)) {
                    pendingCount++;
                } else {
                    visibleCount++;
                }
                totalViewsAndFavs += Math.max(0, b.getViewCount()) + Math.max(0, b.getFavoriteCount());
            }
            hiddenBooksCount = hiddenCount;
            pendingBooksCount = pendingCount;

            if (tvAdminTotalBooks != null) {
                tvAdminTotalBooks.setText(String.valueOf(totalBooksCount));
            }
            if (tvAdminBooksSub != null) {
                tvAdminBooksSub.setText(visibleCount + " hiển thị • " + pendingCount + " chờ duyệt");
            }
            if (btnCheckNow != null) {
                btnCheckNow.setText("Quản lý tin (" + totalBooksCount + ")");
            }

            applyAdminBookFilter();

            if (tvAdminInteractionCount != null) {
                tvAdminInteractionCount.setText("🤝 " + (totalViewsAndFavs + totalRequestsCount) + " Lượt tương tác");
            }

            updateFlaggedAndLogSummary();
        });

        // 3. TC-ADMIN-07: Yêu cầu giao dịch (Đồng bộ Realtime từ RequestRepository kết hợp cả Firestore & Local SharedPreferences)
        RequestRepository.getInstance(this).getAllRequests().observe(this, requests -> {
            if (requests == null) return;
            totalRequestsCount = requests.size();
            int pending = 0;
            int accepted = 0;
            for (Request r : requests) {
                if (Request.STATUS_PENDING.equalsIgnoreCase(r.getStatus())) {
                    pending++;
                } else if (Request.STATUS_ACCEPTED.equalsIgnoreCase(r.getStatus()) || Request.STATUS_COMPLETED.equalsIgnoreCase(r.getStatus())) {
                    accepted++;
                }
            }
            pendingRequestsCount = pending;
            acceptedRequestsCount = accepted;
            if (tvAdminPendingRequests != null) {
                tvAdminPendingRequests.setText(String.valueOf(totalRequestsCount));
            }
            if (tvAdminRequestsSub != null) {
                tvAdminRequestsSub.setText(pending + " chờ duyệt • " + accepted + " hoàn tất");
            }
            updateFlaggedAndLogSummary();
        });

        // 4. Thống kê Lượt lưu yêu thích (favorites collection) từ Firestore
        db.collection("favorites").addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;
            totalFavoritesCount = snap.size();
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

    private void showDatabaseStatusDialog() {
        int activeUsers = Math.max(0, totalUsersCount - bannedUsersCount);
        int visibleBooks = Math.max(0, totalBooksCount - hiddenBooksCount);
        String msg = "☁️ Cloud Firebase Firestore Collections:\n"
                + "• users: " + totalUsersCount + " bản ghi (" + activeUsers + " hoạt động, " + bannedUsersCount + " bị khóa)\n"
                + "• books: " + totalBooksCount + " bản ghi (" + visibleBooks + " hiển thị, " + hiddenBooksCount + " đã ẩn)\n"
                + "• requests: " + totalRequestsCount + " bản ghi (" + pendingRequestsCount + " chờ duyệt, " + acceptedRequestsCount + " hoàn tất)\n"
                + "• favorites: " + totalFavoritesCount + " bản ghi yêu thích\n\n"
                + "💾 Offline Room SQLite (BookRoomDatabase):\n"
                + "• book_table: Đã đồng bộ " + totalBooksCount + " sách cục bộ\n"
                + "• favorite_table: Đang bật chế độ Offline-first (FR12)\n"
                + "• Trạng thái đồng bộ: Hoạt động ổn định (Realtime Sync)";

        new AlertDialog.Builder(this)
                .setTitle("Báo cáo Trạng thái Cơ sở dữ liệu")
                .setMessage(msg)
                .setPositiveButton("Làm mới dữ liệu ngay", (d, w) -> refreshDatabaseMetricsNow())
                .setNeutralButton("Tùy chỉnh kiểm duyệt", (d, w) -> showSystemStatusDialog())
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void refreshDatabaseMetricsNow() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").get().addOnSuccessListener(snap -> {
            if (snap != null) {
                totalUsersCount = snap.size();
                int banned = 0;
                for (DocumentSnapshot d : snap.getDocuments()) {
                    String st = d.getString("status");
                    if ("banned".equalsIgnoreCase(st) || "locked".equalsIgnoreCase(st)) banned++;
                }
                bannedUsersCount = banned;
                if (tvAdminTotalUsers != null) tvAdminTotalUsers.setText(String.valueOf(totalUsersCount));
                updateFlaggedAndLogSummary();
            }
        });
        db.collection("requests").get().addOnSuccessListener(snap -> {
            if (snap != null) {
                totalRequestsCount = snap.size();
                updateFlaggedAndLogSummary();
            }
        });
        db.collection("favorites").get().addOnSuccessListener(snap -> {
            if (snap != null) {
                totalFavoritesCount = snap.size();
                updateFlaggedAndLogSummary();
            }
        });
        RequestRepository.getInstance(this).getAllRequests();
        applyAdminBookFilter();
        loadAdminAvatar();
        Toast.makeText(this, "Đã làm mới và đồng bộ dữ liệu Firestore & Room SQLite!", Toast.LENGTH_SHORT).show();
    }
    private void showSystemStatusDialog() {
        int activeUsers = Math.max(0, totalUsersCount - bannedUsersCount);
        int visibleBooks = Math.max(0, totalBooksCount - hiddenBooksCount);
        String msg = "• Kết nối Cloud: Firebase Firestore & Room SQLite (Trực tuyến)\n"
                + "• Tổng thành viên: " + totalUsersCount + " (" + activeUsers + " hoạt động, " + bannedUsersCount + " bị khóa)\n"
                + "• Tổng bài đăng sách: " + totalBooksCount + " (" + visibleBooks + " hiển thị, " + hiddenBooksCount + " đã ẩn)\n"
                + "• Giao dịch trao đổi: " + totalRequestsCount + " (" + pendingRequestsCount + " chờ duyệt, " + acceptedRequestsCount + " hoàn tất)\n"
                + "• Điểm hẹn mặc định: UEH Cơ sở B (279 Nguyễn Tri Phương, Q.10)\n\n"
                + "Tùy chỉnh chế độ lọc danh sách bài đăng trên Dashboard:";

        String[] filterChoices = new String[]{
                "Tất cả bài đăng sách (" + totalBooksCount + ")",
                "Chỉ sách đang hiển thị (" + Math.max(0, visibleBooks - pendingBooksCount) + ")",
                "Chỉ sách đã ẩn cần kiểm duyệt (" + hiddenBooksCount + ")",
                "Chỉ sách Member mới đăng chờ duyệt (" + pendingBooksCount + ")"
        };

        new AlertDialog.Builder(this)
                .setTitle("Bảng Thông tin & Tùy chỉnh Kiểm duyệt")
                .setMessage(msg)
                .setPositiveButton("Lọc bài đăng sách", (d, w) -> showBookFilterModeDialog(filterChoices))
                .setNeutralButton("Quản lý thành viên", (d, w) ->
                        startActivity(new Intent(AdminDashboardActivity.this, UserManagementActivity.class)))
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void showBookFilterModeDialog(String[] filterChoices) {
        int checked = 0;
        if ("visible".equals(currentBookFilterMode)) checked = 1;
        else if ("hidden".equals(currentBookFilterMode)) checked = 2;
        else if ("pending".equals(currentBookFilterMode)) checked = 3;

        new AlertDialog.Builder(this)
                .setTitle("Chế độ hiển thị sách trên Dashboard")
                .setSingleChoiceItems(filterChoices, checked, (dialog, which) -> {
                    if (which == 0) currentBookFilterMode = "all";
                    else if (which == 1) currentBookFilterMode = "visible";
                    else if (which == 2) currentBookFilterMode = "hidden";
                    else currentBookFilterMode = "pending";
                    currentBookPage = 1;
                    applyAdminBookFilter();
                    dialog.dismiss();
                    if (adminScrollView != null && sectionManageBooks != null) {
                        adminScrollView.smoothScrollTo(0, sectionManageBooks.getTop());
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /** TC-ADMIN-07: Mở màn hình Quản lý & Kiểm duyệt Giao dịch chuẩn đồ án (ManageRequestsActivity) */
    private void openManageRequests() {
        Intent intent = new Intent(AdminDashboardActivity.this, ManageRequestsActivity.class);
        intent.putExtra("is_admin_mode", true);
        startActivity(intent);
    }

    private void showFlaggedItemsDialog() {
        String[] actions = new String[]{
                "Xem danh sách tài khoản bị khóa (" + bannedUsersCount + ")",
                "Lọc xem các bài đăng sách đã ẩn (" + hiddenBooksCount + ")",
                "Hiển thị lại tất cả bài đăng sách (" + totalBooksCount + ")"
        };
        new AlertDialog.Builder(this)
                .setTitle("Kiểm duyệt Bài ẩn & Tài khoản khóa")
                .setItems(actions, (d, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(AdminDashboardActivity.this, UserManagementActivity.class);
                        intent.putExtra("initial_filter", "locked");
                        startActivity(intent);
                    } else if (which == 1) {
                        currentBookFilterMode = "hidden";
                        currentBookPage = 1;
                        applyAdminBookFilter();
                        if (adminScrollView != null && sectionManageBooks != null) {
                            adminScrollView.smoothScrollTo(0, sectionManageBooks.getTop());
                        }
                    } else {
                        currentBookFilterMode = "all";
                        currentBookPage = 1;
                        applyAdminBookFilter();
                        if (adminScrollView != null && sectionManageBooks != null) {
                            adminScrollView.smoothScrollTo(0, sectionManageBooks.getTop());
                        }
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void setupListeners() {
        if (ivAdminAvatar != null) {
            ivAdminAvatar.setOnClickListener(v ->
                    startActivity(new Intent(AdminDashboardActivity.this, ProfileActivity.class))
            );
        }

        if (btnLogoutAdmin != null) {
            btnLogoutAdmin.setOnClickListener(v -> handleLogout());
        }

        if (btnLog != null) {
            btnLog.setOnClickListener(v -> showSystemStatusDialog());
        }

        if (cardSystemLog != null) {
            cardSystemLog.setOnClickListener(v -> showDatabaseStatusDialog());
        }

        View.OnClickListener requestClickListener = v -> openManageRequests();
        if (cardTotalRequests != null) {
            cardTotalRequests.setOnClickListener(requestClickListener);
        }
        if (tvAdminPendingRequests != null) {
            tvAdminPendingRequests.setOnClickListener(requestClickListener);
        }
        if (tvAdminRequestsSub != null) {
            tvAdminRequestsSub.setOnClickListener(requestClickListener);
        }

        if (cardFlaggedItems != null) {
            cardFlaggedItems.setOnClickListener(v -> showFlaggedItemsDialog());
        }

        View.OnClickListener scrollToBooksListener = v -> {
            currentBookFilterMode = "all";
            currentBookPage = 1;
            applyAdminBookFilter();
            if (adminScrollView != null && sectionManageBooks != null) {
                adminScrollView.smoothScrollTo(0, sectionManageBooks.getTop());
            }
        };

        if (btnCheckNow != null) {
            btnCheckNow.setOnClickListener(scrollToBooksListener);
        }

        View.OnClickListener openBookModerationSelector = v -> {
            if (allSystemBooks == null || allSystemBooks.isEmpty()) {
                Toast.makeText(this, "Chưa có bài đăng sách nào để kiểm duyệt.", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] bookTitles = new String[allSystemBooks.size()];
            for (int i = 0; i < allSystemBooks.size(); i++) {
                Book b = allSystemBooks.get(i);
                String statusTag = "hidden".equalsIgnoreCase(b.getStatus()) ? " [Đã ẩn]"
                        : Book.isPendingReview(b) ? " [⏳ Chờ duyệt]" : " [Đang hiển thị]";
                bookTitles[i] = (i + 1) + ". " + (b.getTitle() != null ? b.getTitle() : "Không tiêu đề") + statusTag;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Hộp thoại Kiểm duyệt Sách (" + allSystemBooks.size() + " bài đăng)")
                    .setItems(bookTitles, (d, which) -> {
                        if (which >= 0 && which < allSystemBooks.size()) {
                            showAdminBookModerationDialog(allSystemBooks.get(which));
                        }
                    })
                    .setPositiveButton("Kiểm duyệt bài mới nhất", (d, w) ->
                            showAdminBookModerationDialog(allSystemBooks.get(0)))
                    .setNegativeButton("Đóng", null)
                    .show();
        };

        if (tvAdminViewAllBooks != null) {
            tvAdminViewAllBooks.setOnClickListener(openBookModerationSelector);
        }
        if (sectionManageBooks != null) {
            sectionManageBooks.setOnClickListener(openBookModerationSelector);
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

        if (btnPrevPage != null) {
            btnPrevPage.setOnClickListener(v -> {
                if (currentBookPage > 1) {
                    currentBookPage--;
                    displayCurrentBookPage(true);
                }
            });
        }

        if (btnNextPage != null) {
            btnNextPage.setOnClickListener(v -> {
                int totalPages = Math.max(1, (int) Math.ceil((double) currentFilteredBooks.size() / BOOKS_PAGE_SIZE));
                if (currentBookPage < totalPages) {
                    currentBookPage++;
                    displayCurrentBookPage(true);
                }
            });
        }

        if (tvPageIndicator != null) {
            tvPageIndicator.setOnClickListener(v -> showPagePickerDialog());
        }

        ViewPressEffectHelper.applyPressEffect(
                btnLogoutAdmin, ivAdminAvatar, btnLog, cardSystemLog,
                cardFlaggedItems, cardTotalRequests, tvAdminViewAllBooks,
                cardTotalMembers, cardTotalBooks, btnManageMembers, btnCheckNow, btnViewMemberFeed,
                btnPrevPage, btnNextPage, tvPageIndicator
        );
    }

    private void handleLogout() {
        getSharedPreferences("BookSharePrefs", MODE_PRIVATE).edit()
                .remove("user_fullname")
                .remove("user_avatar_url")
                .remove("user_role")
                .apply();
        authRepository.logout();
        Toast.makeText(this, "Đã đăng xuất khỏi Trang Quản Trị.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(AdminDashboardActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
