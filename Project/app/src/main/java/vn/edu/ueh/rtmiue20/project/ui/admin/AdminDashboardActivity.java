package vn.edu.ueh.rtmiue20.project.ui.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
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

    // Tiêu chí sắp xếp danh sách sách trên Dashboard
    public static final String SORT_NEWEST = "newest";             // Mới nhất (mặc định)
    public static final String SORT_OLDEST = "oldest";             // Cũ nhất
    public static final String SORT_VIEWS_DESC = "views_desc";     // Nhiều lượt xem nhất
    public static final String SORT_FAVS_DESC = "favs_desc";       // Nhiều lượt yêu thích nhất
    public static final String SORT_TITLE_AZ = "title_az";         // Tên sách A → Z
    public static final String SORT_PENDING_FIRST = "pending_first";// Chờ duyệt lên đầu
    private static final String PREF_SORT_MODE = "admin_book_sort_mode";

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
    private TextView tvAdminSortIndicator;
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
    private String currentBookFilterMode = "all"; // all, visible, hidden, pending
    private String currentSortMode = SORT_NEWEST; // newest, oldest, views_desc, favs_desc, title_az, pending_first

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

        SharedPreferences prefs = getSharedPreferences("BookSharePrefs", MODE_PRIVATE);
        prefs.edit()
                .putString("user_role", "admin")
                .apply();

        // Khôi phục tiêu chí sắp xếp đã lưu (nếu có)
        currentSortMode = prefs.getString(PREF_SORT_MODE, SORT_NEWEST);

        authRepository = AuthRepository.getInstance();
        bookViewModel = new ViewModelProvider(this).get(BookViewModel.class);

        initViews();
        setupRecyclerView();
        setupListeners();
        updateSortVisualIndicators();
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
        tvAdminSortIndicator = findViewById(R.id.tvAdminSortIndicator);
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

    /**
     * Sắp xếp danh sách sách theo tiêu chí người dùng chọn
     */
    private void sortBooks(List<Book> list, String sortMode) {
        if (list == null || list.size() <= 1) return;
        Collections.sort(list, (a, b) -> {
            if (SORT_OLDEST.equals(sortMode)) {
                // Cũ nhất: createdAt ASC
                int cmp = Long.compare(a.getCreatedAt(), b.getCreatedAt());
                if (cmp != 0) return cmp;
                return compareTitle(a, b);
            } else if (SORT_VIEWS_DESC.equals(sortMode)) {
                // Nhiều lượt xem nhất: viewCount DESC
                int cmp = Integer.compare(b.getViewCount(), a.getViewCount());
                if (cmp != 0) return cmp;
                return Long.compare(b.getCreatedAt(), a.getCreatedAt());
            } else if (SORT_FAVS_DESC.equals(sortMode)) {
                // Nhiều lượt yêu thích nhất: favoriteCount DESC
                int cmp = Integer.compare(b.getFavoriteCount(), a.getFavoriteCount());
                if (cmp != 0) return cmp;
                return Long.compare(b.getCreatedAt(), a.getCreatedAt());
            } else if (SORT_TITLE_AZ.equals(sortMode)) {
                // Tên sách A → Z
                int cmp = compareTitle(a, b);
                if (cmp != 0) return cmp;
                return Long.compare(b.getCreatedAt(), a.getCreatedAt());
            } else if (SORT_PENDING_FIRST.equals(sortMode)) {
                // Chờ duyệt lên đầu: ưu tiên sách isPendingReview
                int cmp = Boolean.compare(Book.isPendingReview(b), Book.isPendingReview(a));
                if (cmp != 0) return cmp;
                return Long.compare(b.getCreatedAt(), a.getCreatedAt());
            } else {
                // Mặc định: Mới nhất (createdAt DESC)
                int cmp = Long.compare(b.getCreatedAt(), a.getCreatedAt());
                if (cmp != 0) return cmp;
                return compareTitle(a, b);
            }
        });
    }

    private int compareTitle(Book a, Book b) {
        String tA = (a != null && a.getTitle() != null) ? a.getTitle().trim() : "";
        String tB = (b != null && b.getTitle() != null) ? b.getTitle().trim() : "";
        return String.CASE_INSENSITIVE_ORDER.compare(tA, tB);
    }

    /**
     * Lọc & Sắp xếp toàn bộ sách, sau đó cập nhật phân trang
     */
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

        // Áp dụng sắp xếp trên TOÀN BỘ danh sách kết quả đã lọc
        sortBooks(currentFilteredBooks, currentSortMode);

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

    /**
     * Mở BottomSheet "Sắp xếp bài đăng" (Nút Custom Sort)
     */
    private void showSortBottomSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        sheet.setContentView(R.layout.bottom_sheet_admin_sort);

        ImageView btnCloseSortSheet = sheet.findViewById(R.id.btnCloseSortSheet);
        RadioGroup rgSortOptions = sheet.findViewById(R.id.rgSortOptions);
        TextView btnResetSort = sheet.findViewById(R.id.btnResetSort);
        TextView btnApplySort = sheet.findViewById(R.id.btnApplySort);

        if (rgSortOptions == null) return;

        // Chọn RadioButton theo tiêu chí sort hiện tại
        if (SORT_OLDEST.equals(currentSortMode)) {
            rgSortOptions.check(R.id.rbSortOldest);
        } else if (SORT_VIEWS_DESC.equals(currentSortMode)) {
            rgSortOptions.check(R.id.rbSortViews);
        } else if (SORT_FAVS_DESC.equals(currentSortMode)) {
            rgSortOptions.check(R.id.rbSortFavs);
        } else if (SORT_TITLE_AZ.equals(currentSortMode)) {
            rgSortOptions.check(R.id.rbSortTitle);
        } else if (SORT_PENDING_FIRST.equals(currentSortMode)) {
            rgSortOptions.check(R.id.rbSortPending);
        } else {
            rgSortOptions.check(R.id.rbSortNewest);
        }

        final String[] tempSort = new String[]{currentSortMode};
        rgSortOptions.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbSortOldest) {
                tempSort[0] = SORT_OLDEST;
            } else if (checkedId == R.id.rbSortViews) {
                tempSort[0] = SORT_VIEWS_DESC;
            } else if (checkedId == R.id.rbSortFavs) {
                tempSort[0] = SORT_FAVS_DESC;
            } else if (checkedId == R.id.rbSortTitle) {
                tempSort[0] = SORT_TITLE_AZ;
            } else if (checkedId == R.id.rbSortPending) {
                tempSort[0] = SORT_PENDING_FIRST;
            } else {
                tempSort[0] = SORT_NEWEST;
            }
        });

        if (btnCloseSortSheet != null) {
            btnCloseSortSheet.setOnClickListener(v -> sheet.dismiss());
        }

        if (btnResetSort != null) {
            btnResetSort.setOnClickListener(v -> {
                rgSortOptions.check(R.id.rbSortNewest);
                tempSort[0] = SORT_NEWEST;
                applySelectedSort(SORT_NEWEST);
                sheet.dismiss();
                Toast.makeText(AdminDashboardActivity.this, R.string.msg_reset_sort_success, Toast.LENGTH_SHORT).show();
            });
        }

        if (btnApplySort != null) {
            btnApplySort.setOnClickListener(v -> {
                applySelectedSort(tempSort[0]);
                sheet.dismiss();
                Toast.makeText(AdminDashboardActivity.this, R.string.msg_apply_sort_success, Toast.LENGTH_SHORT).show();
            });
        }

        ViewPressEffectHelper.applyPressEffect(btnCloseSortSheet, btnResetSort, btnApplySort);
        sheet.show();
    }

    /**
     * Cập nhật lựa chọn sắp xếp và lưu vào SharedPreferences
     */
    private void applySelectedSort(String sortMode) {
        currentSortMode = sortMode != null ? sortMode : SORT_NEWEST;
        getSharedPreferences("BookSharePrefs", MODE_PRIVATE)
                .edit()
                .putString(PREF_SORT_MODE, currentSortMode)
                .apply();

        // Reset về trang 1 khi đổi tiêu chí sắp xếp
        currentBookPage = 1;
        updateSortVisualIndicators();
        applyAdminBookFilter();
        if (adminScrollView != null && sectionManageBooks != null) {
            adminScrollView.post(() -> adminScrollView.smoothScrollTo(0, sectionManageBooks.getTop()));
        }
    }

    /**
     * Cập nhật trạng thái hiển thị của Icon Sort trên Header và Chip Sort cạnh tiêu đề
     */
    private void updateSortVisualIndicators() {
        boolean isDefault = SORT_NEWEST.equals(currentSortMode);

        // Đổi màu & nền icon nút custom khi khác mặc định
        if (btnLog != null) {
            if (isDefault) {
                btnLog.setColorFilter(ContextCompat.getColor(this, R.color.text_primary));
                btnLog.setBackgroundResource(android.R.color.transparent);
            } else {
                btnLog.setColorFilter(ContextCompat.getColor(this, R.color.on_secondary_container));
                btnLog.setBackgroundResource(R.drawable.bg_tag_secondary);
            }
        }

        // Cập nhật text hiển thị tiêu chí đang chọn
        if (tvAdminSortIndicator != null) {
            String label = getSortLabel(currentSortMode);
            tvAdminSortIndicator.setText(getString(R.string.sort_indicator_format, label));
            if (isDefault) {
                tvAdminSortIndicator.setBackgroundResource(R.drawable.bg_pill_inactive);
                tvAdminSortIndicator.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            } else {
                tvAdminSortIndicator.setBackgroundResource(R.drawable.bg_tag_secondary);
                tvAdminSortIndicator.setTextColor(ContextCompat.getColor(this, R.color.on_secondary_container));
            }
        }
    }

    private String getSortLabel(String sortMode) {
        if (SORT_OLDEST.equals(sortMode)) return getString(R.string.sort_label_oldest);
        if (SORT_VIEWS_DESC.equals(sortMode)) return getString(R.string.sort_label_views);
        if (SORT_FAVS_DESC.equals(sortMode)) return getString(R.string.sort_label_favs);
        if (SORT_TITLE_AZ.equals(sortMode)) return getString(R.string.sort_label_title);
        if (SORT_PENDING_FIRST.equals(sortMode)) return getString(R.string.sort_label_pending);
        return getString(R.string.sort_label_newest);
    }

    private void showAdminBookModerationDialog(Book book) {
        if (book == null) return;

        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        sheet.setContentView(R.layout.bottom_sheet_admin_book_moderation);

        ImageView btnClose = sheet.findViewById(R.id.btnCloseModerationSheet);
        ImageView ivCover = sheet.findViewById(R.id.ivModerationBookCover);
        TextView tvTitle = sheet.findViewById(R.id.tvModerationBookTitle);
        TextView tvAuthor = sheet.findViewById(R.id.tvModerationBookAuthor);
        TextView tvOwner = sheet.findViewById(R.id.tvModerationBookOwner);
        TextView tvStatusBadge = sheet.findViewById(R.id.tvModerationStatusBadge);

        View btnActionView = sheet.findViewById(R.id.btnActionViewDetail);
        View btnActionApprove = sheet.findViewById(R.id.btnActionApproveBook);
        View btnActionHide = sheet.findViewById(R.id.btnActionHideBook);
        View btnActionDelete = sheet.findViewById(R.id.btnActionDeleteBook);
        View btnCancel = sheet.findViewById(R.id.btnCancelModerationSheet);

        // Hiển thị thông tin sách vắn tắt
        if (tvTitle != null) {
            tvTitle.setText(book.getTitle() != null ? book.getTitle() : "Bài đăng sách");
        }
        if (tvAuthor != null) {
            String author = (book.getAuthor() != null && !book.getAuthor().trim().isEmpty())
                    ? book.getAuthor() : "Chưa rõ tác giả";
            tvAuthor.setText("Tác giả: " + author);
        }
        if (tvOwner != null) {
            String cat = (book.getCategory() != null && !book.getCategory().trim().isEmpty())
                    ? book.getCategory() : "Chia sẻ sách";
            String cond = (book.getCondition() != null && !book.getCondition().trim().isEmpty())
                    ? book.getCondition() : "Đã qua sử dụng";
            tvOwner.setText(cat + " • " + cond);

            if (book.getOwnerId() != null && !book.getOwnerId().trim().isEmpty()) {
                UserRepository.getInstance().getUserProfile(book.getOwnerId(), new UserRepository.UserCallback() {
                    @Override
                    public void onSuccess(User u) {
                        if (u != null && u.getFullName() != null && !u.getFullName().trim().isEmpty()) {
                            tvOwner.setText("Người đăng: " + u.getFullName());
                        }
                    }

                    @Override
                    public void onError(String error) {}
                });
            }
        }

        // Bìa sách
        if (ivCover != null) {
            if (book.getImageUrl() != null && !book.getImageUrl().trim().isEmpty()) {
                com.bumptech.glide.Glide.with(this)
                        .load(book.getImageUrl())
                        .placeholder(R.drawable.ic_bookshare_logo)
                        .error(R.drawable.ic_bookshare_logo)
                        .into(ivCover);
            } else {
                ivCover.setImageResource(R.drawable.ic_bookshare_logo);
            }
        }

        // Huy hiệu trạng thái sách
        boolean isPending = Book.isPendingReview(book);
        boolean isHidden = "hidden".equalsIgnoreCase(book.getStatus());
        if (tvStatusBadge != null) {
            if (isPending) {
                tvStatusBadge.setText("⏳ Chờ duyệt");
                tvStatusBadge.setBackgroundResource(R.drawable.bg_tag_secondary);
                tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.tertiary_container));
            } else if (isHidden) {
                tvStatusBadge.setText("🚫 Đã ẩn vi phạm");
                tvStatusBadge.setBackgroundResource(R.drawable.bg_moderation_action_danger);
                tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.error));
            } else {
                tvStatusBadge.setText("✅ Đang hiển thị");
                tvStatusBadge.setBackgroundResource(R.drawable.bg_tag_secondary);
                tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.on_secondary_container));
            }
        }

        TextView tvApproveTitle = sheet.findViewById(R.id.tvActionApproveTitle);
        if (tvApproveTitle != null && !isPending && !isHidden) {
            tvApproveTitle.setText("Cập nhật phê duyệt (Đang hiển thị)");
        }

        // Khối 1: Xem chi tiết
        if (btnActionView != null) {
            btnActionView.setOnClickListener(v -> {
                sheet.dismiss();
                Intent intent = new Intent(AdminDashboardActivity.this, BookDetailActivity.class);
                intent.putExtra("bookId", book.getId());
                startActivity(intent);
            });
        }

        // Khối 2: Phê duyệt
        if (btnActionApprove != null) {
            btnActionApprove.setOnClickListener(v -> {
                sheet.dismiss();
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
            });
        }

        // Khối 3: Từ chối / Tạm ẩn
        if (btnActionHide != null) {
            btnActionHide.setOnClickListener(v -> {
                sheet.dismiss();
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
            });
        }

        // Khối 4: Xóa vĩnh viễn
        if (btnActionDelete != null) {
            btnActionDelete.setOnClickListener(v -> {
                sheet.dismiss();
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
            });
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> sheet.dismiss());
        }
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> sheet.dismiss());
        }

        ViewPressEffectHelper.applyPressEffect(btnClose, btnCancel, btnActionView, btnActionApprove, btnActionHide, btnActionDelete);
        sheet.show();
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
                btnCheckNow.setText("Quản lý bài đăng (" + totalBooksCount + ")");
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
            int completed = 0;
            for (Request r : requests) {
                if (Request.STATUS_PENDING.equalsIgnoreCase(r.getStatus())) {
                    pending++;
                } else if (Request.STATUS_COMPLETED.equalsIgnoreCase(r.getStatus())) {
                    completed++;
                } else if (Request.STATUS_ACCEPTED.equalsIgnoreCase(r.getStatus())) {
                    accepted++;
                }
            }
            pendingRequestsCount = pending;
            acceptedRequestsCount = completed + accepted;
            if (tvAdminPendingRequests != null) {
                tvAdminPendingRequests.setText(String.valueOf(totalRequestsCount));
            }
            if (tvAdminRequestsSub != null) {
                if (completed > 0) {
                    tvAdminRequestsSub.setText(pending + " chờ duyệt • " + completed + " hoàn tất");
                } else {
                    tvAdminRequestsSub.setText(pending + " chờ duyệt • " + accepted + " chấp nhận");
                }
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

        com.google.android.material.bottomsheet.BottomSheetDialog sheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        sheet.setContentView(R.layout.bottom_sheet_admin_database_status);

        ImageView btnClose = sheet.findViewById(R.id.btnCloseDbStatusSheet);
        View btnRefresh = sheet.findViewById(R.id.btnRefreshDbMetrics);
        View btnCancel = sheet.findViewById(R.id.btnCancelDbStatusSheet);

        TextView tvUsersCount = sheet.findViewById(R.id.tvDbUsersCount);
        TextView tvUsersActive = sheet.findViewById(R.id.tvDbUsersActive);
        TextView tvUsersLocked = sheet.findViewById(R.id.tvDbUsersLocked);

        TextView tvBooksCount = sheet.findViewById(R.id.tvDbBooksCount);
        TextView tvBooksVisible = sheet.findViewById(R.id.tvDbBooksVisible);
        TextView tvBooksHidden = sheet.findViewById(R.id.tvDbBooksHidden);

        TextView tvRequestsCount = sheet.findViewById(R.id.tvDbRequestsCount);
        TextView tvRequestsPending = sheet.findViewById(R.id.tvDbRequestsPending);
        TextView tvRequestsCompleted = sheet.findViewById(R.id.tvDbRequestsCompleted);

        TextView tvFavoritesCount = sheet.findViewById(R.id.tvDbFavoritesCount);
        TextView tvRoomBooksDetail = sheet.findViewById(R.id.tvRoomBooksDetail);

        // Bind metrics vào từng khối thẻ
        if (tvUsersCount != null) tvUsersCount.setText(totalUsersCount + " bản ghi");
        if (tvUsersActive != null) tvUsersActive.setText(activeUsers + " hoạt động");
        if (tvUsersLocked != null) {
            tvUsersLocked.setText(bannedUsersCount + " bị khóa");
            if (bannedUsersCount == 0) {
                tvUsersLocked.setBackgroundResource(R.drawable.bg_tag_grey);
                tvUsersLocked.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }
        }

        if (tvBooksCount != null) tvBooksCount.setText(totalBooksCount + " bản ghi");
        if (tvBooksVisible != null) tvBooksVisible.setText(visibleBooks + " hiển thị");
        if (tvBooksHidden != null) {
            tvBooksHidden.setText(hiddenBooksCount + " đã ẩn");
            if (hiddenBooksCount == 0) {
                tvBooksHidden.setBackgroundResource(R.drawable.bg_tag_grey);
                tvBooksHidden.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }
        }

        if (tvRequestsCount != null) tvRequestsCount.setText(totalRequestsCount + " bản ghi");
        if (tvRequestsPending != null) tvRequestsPending.setText(pendingRequestsCount + " chờ duyệt");
        if (tvRequestsCompleted != null) tvRequestsCompleted.setText(acceptedRequestsCount + " hoàn tất");

        if (tvFavoritesCount != null) tvFavoritesCount.setText(totalFavoritesCount + " bản ghi");
        if (tvRoomBooksDetail != null) tvRoomBooksDetail.setText("book_table: Đã đồng bộ " + totalBooksCount + " sách cục bộ");

        if (btnClose != null) btnClose.setOnClickListener(v -> sheet.dismiss());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> sheet.dismiss());

        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                sheet.dismiss();
                refreshDatabaseMetricsNow();
            });
        }

        ViewPressEffectHelper.applyPressEffect(btnClose, btnCancel, btnRefresh);
        sheet.show();
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

        // Bấm nút custom trên Header: Mở BottomSheet Sắp xếp bài đăng sách
        if (btnLog != null) {
            btnLog.setOnClickListener(v -> showSortBottomSheet());
        }

        // Bấm chip tiêu chí sắp xếp cạnh tiêu đề: Cũng mở BottomSheet Sắp xếp
        if (tvAdminSortIndicator != null) {
            tvAdminSortIndicator.setOnClickListener(v -> showSortBottomSheet());
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
                btnPrevPage, btnNextPage, tvPageIndicator, tvAdminSortIndicator
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
