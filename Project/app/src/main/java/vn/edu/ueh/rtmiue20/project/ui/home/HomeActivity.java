package vn.edu.ueh.rtmiue20.project.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import vn.edu.ueh.rtmiue20.project.data.repository.AuthRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.Book;
import vn.edu.ueh.rtmiue20.project.data.model.Request;
import vn.edu.ueh.rtmiue20.project.data.model.NotificationItem;
import vn.edu.ueh.rtmiue20.project.data.repository.NotificationRepository;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import vn.edu.ueh.rtmiue20.project.ui.admin.AdminDashboardActivity;
import vn.edu.ueh.rtmiue20.project.ui.auth.LogInActivity;
import vn.edu.ueh.rtmiue20.project.ui.book.BookDetailActivity;
import vn.edu.ueh.rtmiue20.project.ui.book.CreatePostActivity;
import vn.edu.ueh.rtmiue20.project.ui.favorite.FavoriteViewModel;
import vn.edu.ueh.rtmiue20.project.ui.profile.ProfileActivity;
import vn.edu.ueh.rtmiue20.project.ui.request.ManageRequestsActivity;
import vn.edu.ueh.rtmiue20.project.ui.request.RequestViewModel;
import vn.edu.ueh.rtmiue20.project.util.LocationHelper;
import vn.edu.ueh.rtmiue20.project.util.ViewPressEffectHelper;

public class HomeActivity extends AppCompatActivity {

    private BookAdapter bookAdapter;
    private List<Book> allBooksList = new ArrayList<>();
    private List<Book> currentFilteredList = new ArrayList<>();

    private ScrollView mainScrollView;
    private EditText etSearchBook;
    private ImageView btnClearSearch;
    private RecyclerView rvBooks;
    private TextView tvEmptyBooks;
    private TextView tvBookCountBadge;

    private TextView btnBackToAdminDashboard;
    private ImageView btnNotifications;
    private TextView tvNotificationBadge;
    private ImageView btnGoToProfile;
    private View btnFilter;
    private View layoutBrandLogo, ivBrandLogoIcon;

    // Category pills
    private TextView pillAll, pillLiterature, pillEconomics, pillTextbook, pillOther;
    private String selectedCategory = "Tất cả";

    // Filter states
    private String filterCondition = "Tất cả";
    private String filterSharingType = "Tất cả";
    private String filterFavoriteMode = "Tất cả sách";
    private String filterSortOrder = "Mới nhất";
    private Set<String> currentFavoriteBookIds = new HashSet<>();

    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    // Bottom Navigation & Actions
    private View btnNavHome, btnNavSearch, btnNavRequests, btnNavProfile;
    private ImageView fabAddBook;

    // Pagination
    private static final int ITEMS_PER_PAGE = 4;
    private int currentPage = 1;
    private int totalPages = 1;
    private TextView btnPrevPage, btnNextPage, tvPageIndicator;

    // Real-time notification state
    private int pendingReceivedCount = 0;
    private int acceptedSentCount = 0;
    private final List<Request> rawReceivedRequests = new ArrayList<>();
    private final List<Request> rawSentRequests = new ArrayList<>();
    private final List<NotificationItem> firestoreNotifications = new ArrayList<>();
    private int unreadNotificationsCount = 0;
    private int previousTotalBooksCount = -1;
    private ListenerRegistration userStatusListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_member_dashboard);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainMemberDashboard), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupRecyclerView();

        BookViewModel bookViewModel = new ViewModelProvider(this).get(BookViewModel.class);
        bookViewModel.getAllBooks().observe(this, books -> {
            if (books != null) {
                boolean addedNewBook = previousTotalBooksCount >= 0 && books.size() > previousTotalBooksCount;
                previousTotalBooksCount = books.size();
                allBooksList = books;
                if (addedNewBook) {
                    currentPage = 1;
                    filterSortOrder = "Mới nhất";
                    if (!"Tất cả".equals(selectedCategory)) {
                        selectCategory("Tất cả", pillAll);
                        return;
                    }
                }
                applyFilters();
            }
        });

        FavoriteViewModel favoriteViewModel = new ViewModelProvider(this).get(FavoriteViewModel.class);
        favoriteViewModel.getFavoriteBookIds().observe(this, ids -> {
            currentFavoriteBookIds = ids != null ? new HashSet<>(ids) : new HashSet<>();
            if (bookAdapter != null && ids != null) {
                bookAdapter.setFavoritedBookIds(ids);
            }
            if (!"Tất cả sách".equals(filterFavoriteMode)) {
                applyFilters();
            }
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationPermissionLauncher();
        setupNotificationPermissionLauncher();

        observeNotificationData();
        setupListeners();
        setupPressEffects();
        updateUserDistanceToExchange();
        loadHeaderUserAvatar();
        syncFcmTokenToFirestore();
        requestLocationOnAppStart();
        requestNotificationPermissionOnStart();
        handleFocusSearchIntent(getIntent());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (userStatusListener != null) {
            userStatusListener.remove();
            userStatusListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userStatusListener != null) {
            userStatusListener.remove();
            userStatusListener = null;
        }
    }

    private void checkAndListenUserStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        if (userStatusListener != null) {
            userStatusListener.remove();
        }

        userStatusListener = FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        if (error.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            handleAccountLocked("Tài khoản của bạn đã bị khóa hoặc không có quyền truy cập hệ thống.");
                        }
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        String status = snapshot.getString("status");
                        if ("locked".equalsIgnoreCase(status) || "banned".equalsIgnoreCase(status)) {
                            String reason = snapshot.getString("lockReason");
                            handleAccountLocked(reason != null && !reason.trim().isEmpty() ? reason : "Tài khoản vi phạm quy chuẩn cộng đồng");
                        }
                    }
                });
    }

    private void handleAccountLocked(String reason) {
        if (isFinishing() || isDestroyed()) return;
        if (userStatusListener != null) {
            userStatusListener.remove();
            userStatusListener = null;
        }
        AuthRepository.getInstance().logout();
        getSharedPreferences("BookSharePrefs", MODE_PRIVATE).edit().putBoolean("remember_me", false).apply();
        Intent intent = new Intent(this, LogInActivity.class);
        intent.putExtra("LOCKED_ERROR", "Tài khoản của bạn đã bị khóa: " + reason);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleFocusSearchIntent(intent);
    }

    private void handleFocusSearchIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("focus_search", false)) {
            intent.removeExtra("focus_search");
            if (etSearchBook != null) {
                etSearchBook.postDelayed(() -> {
                    if (mainScrollView != null) {
                        mainScrollView.smoothScrollTo(0, etSearchBook.getTop());
                    }
                    etSearchBook.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(etSearchBook, InputMethodManager.SHOW_IMPLICIT);
                    }
                }, 200);
            }
        }
    }

    private void observeNotificationData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // 1. Lắng nghe Notifications thời gian thực từ Firestore collection "notifications"
        NotificationRepository.getInstance().getNotifications(user.getUid()).observe(this, list -> {
            firestoreNotifications.clear();
            if (list != null) {
                firestoreNotifications.addAll(list);
            }
            updateNotificationBadge();
        });

        NotificationRepository.getInstance().getUnreadCount(user.getUid()).observe(this, count -> {
            unreadNotificationsCount = count != null ? count : 0;
            updateNotificationBadge();
        });

        // 2. Lắng nghe các Request hiện có làm nguồn đối chiếu/fallback
        RequestViewModel requestViewModel = new ViewModelProvider(this).get(RequestViewModel.class);
        requestViewModel.getReceivedRequests(user.getUid()).observe(this, list -> {
            rawReceivedRequests.clear();
            int pending = 0;
            if (list != null) {
                rawReceivedRequests.addAll(list);
                for (Request r : list) {
                    if (Request.STATUS_PENDING.equalsIgnoreCase(r.getStatus())) {
                        pending++;
                    }
                }
            }
            pendingReceivedCount = pending;
            updateNotificationBadge();
        });

        requestViewModel.getSentRequests(user.getUid()).observe(this, list -> {
            rawSentRequests.clear();
            int accepted = 0;
            if (list != null) {
                rawSentRequests.addAll(list);
                for (Request r : list) {
                    if (Request.STATUS_ACCEPTED.equalsIgnoreCase(r.getStatus()) || Request.STATUS_COMPLETED.equalsIgnoreCase(r.getStatus())) {
                        accepted++;
                    }
                }
            }
            acceptedSentCount = accepted;
            updateNotificationBadge();
        });
    }

    private List<NotificationItem> buildNotificationList() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUid = currentUser != null ? currentUser.getUid() : "";

        // Ưu tiên danh sách đồng bộ từ Firestore collection "notifications"
        Map<String, NotificationItem> merged = new LinkedHashMap<>();
        for (NotificationItem item : firestoreNotifications) {
            if (item != null && item.getId() != null) {
                merged.put(item.getId(), item);
            }
        }

        // Bổ sung các thông báo từ yêu cầu nhận sách (fallback nếu Firestore notifications đang load)
        for (Request r : rawReceivedRequests) {
            if (r == null || r.getRequestId() == null) continue;
            String fallbackId = "req_recv_" + r.getRequestId();
            if (merged.containsKey(fallbackId) || merged.containsKey(r.getRequestId())) continue;

            String status = r.getStatus() != null ? r.getStatus() : "";
            String rName = (r.getRequesterName() != null && !r.getRequesterName().trim().isEmpty())
                    ? r.getRequesterName().trim()
                    : "Một bạn đọc";
            String bTitle = r.getBookTitle() != null ? r.getBookTitle() : "sách";
            String avatar = (r.getRequesterAvatar() != null && !r.getRequesterAvatar().trim().isEmpty())
                    ? r.getRequesterAvatar()
                    : r.getBookImageUrl();
            String subMsg = (r.getMessage() != null && !r.getMessage().trim().isEmpty())
                    ? "\"" + r.getMessage().trim() + "\""
                    : null;

            if (Request.STATUS_PENDING.equalsIgnoreCase(status)) {
                merged.put(fallbackId, new NotificationItem(
                        fallbackId,
                        currentUid,
                        r.getRequesterId(),
                        rName,
                        NotificationItem.TYPE_REQUEST_RECEIVED,
                        "Yêu cầu nhận sách mới",
                        rName + " đã gửi yêu cầu nhận cuốn \"" + bTitle + "\"",
                        subMsg,
                        r.getTimestamp(),
                        avatar,
                        "Chờ phản hồi",
                        true,
                        false,
                        "REQUESTS",
                        "received",
                        r.getRequestId(),
                        r.getBookId()
                ));
            }
        }

        // Bổ sung các yêu cầu người dùng đi xin sách
        for (Request r : rawSentRequests) {
            if (r == null || r.getRequestId() == null) continue;
            String fallbackId = "req_sent_" + r.getRequestId();
            if (merged.containsKey("req_acc_" + r.getRequestId())
                    || merged.containsKey("req_rej_" + r.getRequestId())
                    || merged.containsKey("req_comp_r_" + r.getRequestId())
                    || merged.containsKey(fallbackId)) {
                continue;
            }

            String status = r.getStatus() != null ? r.getStatus() : "";
            String oName = (r.getOwnerName() != null && !r.getOwnerName().trim().isEmpty())
                    ? r.getOwnerName().trim()
                    : "Chủ sách";
            String bTitle = r.getBookTitle() != null ? r.getBookTitle() : "sách";
            String cover = (r.getBookImageUrl() != null && !r.getBookImageUrl().trim().isEmpty())
                    ? r.getBookImageUrl()
                    : r.getRequesterAvatar();

            if (Request.STATUS_ACCEPTED.equalsIgnoreCase(status)) {
                merged.put(fallbackId, new NotificationItem(
                        fallbackId,
                        currentUid,
                        r.getOwnerId(),
                        oName,
                        NotificationItem.TYPE_REQUEST_ACCEPTED,
                        "Yêu cầu được chấp nhận! 🎉",
                        oName + " đã đồng ý tặng cuốn \"" + bTitle + "\" cho bạn. Hãy liên hệ hẹn nhận sách!",
                        "Điểm hẹn: Đại học Kinh Tế TP.HCM (UEH Cơ sở B)",
                        r.getTimestamp(),
                        cover,
                        "Đã chấp nhận",
                        true,
                        false,
                        "REQUESTS",
                        "sent",
                        r.getRequestId(),
                        r.getBookId()
                ));
            } else if (Request.STATUS_COMPLETED.equalsIgnoreCase(status)) {
                merged.put(fallbackId, new NotificationItem(
                        fallbackId,
                        currentUid,
                        r.getOwnerId(),
                        oName,
                        NotificationItem.TYPE_REQUEST_COMPLETED,
                        "Đã nhận sách thành công ✅",
                        "Bạn đã nhận thành công cuốn \"" + bTitle + "\" từ " + oName + ".",
                        !r.isRated() ? "Chạm để đánh giá uy tín cho chủ sách" : null,
                        r.getCompletedTimestamp() > 0 ? r.getCompletedTimestamp() : r.getTimestamp(),
                        cover,
                        "Đã nhận",
                        !r.isRated(),
                        false,
                        "REQUESTS",
                        "sent",
                        r.getRequestId(),
                        r.getBookId()
                ));
            }
        }

        // Bổ sung thông báo kiểm duyệt bài đăng cá nhân
        if (allBooksList != null && !currentUid.isEmpty()) {
            for (Book b : allBooksList) {
                if (b == null || b.getOwnerId() == null || !b.getOwnerId().equals(currentUid)) continue;
                String notifId = "book_status_" + b.getId();
                if (merged.containsKey(notifId)) continue;

                if (Book.isPendingReview(b)) {
                    merged.put(notifId, new NotificationItem(
                            notifId,
                            currentUid,
                            "admin",
                            "Quản trị viên",
                            NotificationItem.TYPE_BOOK_PENDING,
                            "Bài đăng chờ duyệt",
                            "Cuốn \"" + b.getTitle() + "\" của bạn đang chờ Quản trị viên duyệt để lên trang chủ.",
                            null,
                            b.getCreatedAt(),
                            b.getImageUrl(),
                            "Chờ duyệt",
                            false,
                            false,
                            "BOOK_DETAIL",
                            "",
                            null,
                            b.getId()
                    ));
                } else if (Book.STATUS_HIDDEN.equalsIgnoreCase(b.getStatus())) {
                    merged.put(notifId, new NotificationItem(
                            notifId,
                            currentUid,
                            "admin",
                            "Quản trị viên",
                            NotificationItem.TYPE_BOOK_HIDDEN,
                            "Bài đăng đã bị ẩn",
                            "Cuốn \"" + b.getTitle() + "\" đã bị Quản trị viên ẩn do vi phạm quy chuẩn cộng đồng.",
                            null,
                            b.getCreatedAt(),
                            b.getImageUrl(),
                            "Đã ẩn",
                            false,
                            false,
                            "BOOK_DETAIL",
                            "",
                            null,
                            b.getId()
                    ));
                }
            }
        }

        List<NotificationItem> result = new ArrayList<>(merged.values());
        Collections.sort(result, (n1, n2) -> {
            if (n1.isActionRequired() != n2.isActionRequired()) {
                return n1.isActionRequired() ? -1 : 1;
            }
            return Long.compare(n2.getTimestamp(), n1.getTimestamp());
        });

        return result;
    }

    private void updateNotificationBadge() {
        if (tvNotificationBadge == null) return;
        int total = unreadNotificationsCount;
        if (total <= 0 && firestoreNotifications.isEmpty()) {
            total = pendingReceivedCount + acceptedSentCount;
        }
        if (total > 0) {
            tvNotificationBadge.setText(total > 99 ? "99+" : String.valueOf(total));
            tvNotificationBadge.setVisibility(View.VISIBLE);
        } else {
            tvNotificationBadge.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndListenUserStatus();
        updateUserDistanceToExchange();
        loadHeaderUserAvatar();
        syncFcmTokenToFirestore();
    }

    private void loadHeaderUserAvatar() {
        android.content.SharedPreferences prefs = getSharedPreferences("BookSharePrefs", MODE_PRIVATE);
        if (btnBackToAdminDashboard != null) {
            boolean isCachedAdmin = "admin".equalsIgnoreCase(prefs.getString("user_role", ""));
            btnBackToAdminDashboard.setVisibility(isCachedAdmin ? View.VISIBLE : View.GONE);
        }

        if (btnGoToProfile != null && prefs.contains("user_avatar_url")) {
            String cachedAvatar = prefs.getString("user_avatar_url", "");
            vn.edu.ueh.rtmiue20.project.util.ImageHelper.bindAvatar(this, btnGoToProfile, cachedAvatar, true);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            vn.edu.ueh.rtmiue20.project.data.repository.UserRepository.getInstance().getUserProfile(
                    user.getUid(),
                    new vn.edu.ueh.rtmiue20.project.data.repository.UserRepository.UserCallback() {
                        @Override
                        public void onSuccess(vn.edu.ueh.rtmiue20.project.data.model.User userProfile) {
                            if (isFinishing() || isDestroyed()) return;
                            String avatarUrl = (userProfile != null && userProfile.getAvatarUrl() != null)
                                    ? userProfile.getAvatarUrl()
                                    : "";
                            String role = (userProfile != null && userProfile.getRole() != null)
                                    ? userProfile.getRole()
                                    : "member";
                            prefs.edit()
                                    .putString("user_avatar_url", avatarUrl)
                                    .putString("user_role", role)
                                    .apply();
                            if (btnBackToAdminDashboard != null) {
                                btnBackToAdminDashboard.setVisibility("admin".equalsIgnoreCase(role) ? View.VISIBLE : View.GONE);
                            }
                            if (btnGoToProfile != null) {
                                vn.edu.ueh.rtmiue20.project.util.ImageHelper.bindAvatar(HomeActivity.this, btnGoToProfile, avatarUrl, true);
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {}
                    }
            );
        }
    }

    private void setupLocationPermissionLauncher() {
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    if ((fineGranted != null && fineGranted) || (coarseGranted != null && coarseGranted)) {
                        fetchCurrentGpsDistance();
                    }
                }
        );
    }

    private void requestLocationOnAppStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentGpsDistance();
        } else if (locationPermissionLauncher != null) {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void setupNotificationPermissionLauncher() {
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> syncFcmTokenToFirestore()
        );
    }

    private void requestNotificationPermissionOnStart() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                if (notificationPermissionLauncher != null) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            } else {
                syncFcmTokenToFirestore();
            }
        } else {
            syncFcmTokenToFirestore();
        }
    }

    private void syncFcmTokenToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w("FCM", "syncFcmTokenToFirestore: Chưa đăng nhập, bỏ qua đồng bộ FCM token");
            return;
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("FCM", "Không thể lấy FCM Token từ Firebase", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    Log.i("FCM", "Lấy FCM Token thành công: " + token);
                    if (token != null && !token.isEmpty()) {
                        Map<String, Object> tokenData = new HashMap<>();
                        tokenData.put("fcmToken", token);
                        FirebaseFirestore.getInstance().collection("users")
                                .document(user.getUid())
                                .set(tokenData, SetOptions.merge())
                                .addOnSuccessListener(unused -> Log.i("FCM", "Đã lưu fcmToken lên Firestore cho user: " + user.getUid()))
                                .addOnFailureListener(e -> Log.e("FCM", "Lỗi lưu fcmToken lên Firestore: " + e.getMessage(), e));
                    }
                });
    }

    @SuppressLint("MissingPermission")
    private void fetchCurrentGpsDistance() {
        if (fusedLocationClient == null) return;
        try {
            CurrentLocationRequest request = new CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    .setDurationMillis(8000)
                    .build();
            fusedLocationClient.getCurrentLocation(request, null).addOnSuccessListener(this, location -> {
                if (location != null) {
                    double safeLat = LocationHelper.normalizeLatitude(location.getLatitude(), location.getLongitude());
                    double safeLng = LocationHelper.normalizeLongitude(location.getLatitude(), location.getLongitude());
                    getSharedPreferences("BookSharePrefs", MODE_PRIVATE).edit()
                            .putFloat("user_lat", (float) safeLat)
                            .putFloat("user_lng", (float) safeLng)
                            .apply();
                    String dist = LocationHelper.getDistanceToUEHCampusB(safeLat, safeLng); double uLat = safeLat; double uLng = safeLng;
                    if (bookAdapter != null) {
                        bookAdapter.setUserDistanceToExchange(dist);
                                bookAdapter.setCurrentUserCoordinates(uLat, uLng);
                    }
                }
            });
        } catch (SecurityException ignored) {}
    }

    private void updateUserDistanceToExchange() {
        android.content.SharedPreferences prefs = getSharedPreferences("BookSharePrefs", MODE_PRIVATE);
        float lat = prefs.getFloat("user_lat", 0f);
        float lng = prefs.getFloat("user_lng", 0f);

        if (lat != 0f && lng != 0f) {
            String dist = vn.edu.ueh.rtmiue20.project.util.LocationHelper.getDistanceToUEHCampusB(lat, lng);
            if (bookAdapter != null) {
                bookAdapter.setCurrentUserCoordinates(lat, lng);
            }
            if (bookAdapter != null) {
                bookAdapter.setUserDistanceToExchange(dist);
            }
        } else {
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                vn.edu.ueh.rtmiue20.project.data.repository.UserRepository.getInstance().getUserProfile(user.getUid(), new vn.edu.ueh.rtmiue20.project.data.repository.UserRepository.UserCallback() {
                    @Override
                    public void onSuccess(vn.edu.ueh.rtmiue20.project.data.model.User userProfile) {
                        if (userProfile != null && userProfile.getLatitude() != null && userProfile.getLongitude() != null) {
                            float uLat = userProfile.getLatitude().floatValue();
                            float uLng = userProfile.getLongitude().floatValue();
                            prefs.edit().putFloat("user_lat", uLat).putFloat("user_lng", uLng).apply();
                            String dist = vn.edu.ueh.rtmiue20.project.util.LocationHelper.getDistanceToUEHCampusB(uLat, uLng);
                            if (bookAdapter != null) {
                                bookAdapter.setUserDistanceToExchange(dist);
                                bookAdapter.setCurrentUserCoordinates(uLat, uLng);
                            }
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {}
                });
            }
        }
    }

    private void initViews() {
        mainScrollView = findViewById(R.id.mainScrollView);
        etSearchBook = findViewById(R.id.etSearchBook);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        rvBooks = findViewById(R.id.rvBooks);
        tvEmptyBooks = findViewById(R.id.tvEmptyBooks);
        tvBookCountBadge = findViewById(R.id.tvBookCountBadge);

        btnBackToAdminDashboard = findViewById(R.id.btnBackToAdminDashboard);
        btnNotifications = findViewById(R.id.btnNotifications);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        btnGoToProfile = findViewById(R.id.btnGoToProfile);
        btnFilter = findViewById(R.id.btnFilter);
        layoutBrandLogo = findViewById(R.id.layoutBrandLogo);
        ivBrandLogoIcon = findViewById(R.id.ivBrandLogoIcon);

        pillAll = findViewById(R.id.pillAll);
        pillLiterature = findViewById(R.id.pillLiterature);
        pillEconomics = findViewById(R.id.pillEconomics);
        pillTextbook = findViewById(R.id.pillTextbook);
        pillOther = findViewById(R.id.pillOther);

        btnNavHome = findViewById(R.id.btnNavHome);
        btnNavSearch = findViewById(R.id.btnNavSearch);
        btnNavRequests = findViewById(R.id.btnNavRequests);
        btnNavProfile = findViewById(R.id.btnNavProfile);
        fabAddBook = findViewById(R.id.fabAddBook);

        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        tvPageIndicator = findViewById(R.id.tvPageIndicator);
    }

    private void setupRecyclerView() {
        int columns = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 4 : 2;
        rvBooks.setLayoutManager(new GridLayoutManager(this, columns));
        bookAdapter = new BookAdapter(book -> {
            Intent intent = new Intent(HomeActivity.this, BookDetailActivity.class);
            intent.putExtra("bookId", book.getId());
            startActivity(intent);
        });
        rvBooks.setAdapter(bookAdapter);
    }

    private boolean checkGuestLogin() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thực hiện tính năng này!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LogInActivity.class));
            return false;
        }
        return true;
    }

    private void setupListeners() {
        if (btnBackToAdminDashboard != null) {
            btnBackToAdminDashboard.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, AdminDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        // Search text watcher
        etSearchBook.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(s.length() != 0 ? View.VISIBLE : View.GONE);
                currentPage = 1;
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearchBook.setText("");
            etSearchBook.clearFocus();
        });

        // Notifications Bell
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> showNotificationsDialog());
        }

        // Profile Buttons
        if (btnGoToProfile != null) {
            btnGoToProfile.setOnClickListener(v -> {
                if (!checkGuestLogin()) return;
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
            });
        }
        if (btnNavProfile != null) {
            btnNavProfile.setOnClickListener(v -> {
                if (!checkGuestLogin()) return;
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
            });
        }

        // FAB Add Book
        if (fabAddBook != null) {
            fabAddBook.setOnClickListener(v -> {
                if (!checkGuestLogin()) return;
                startActivity(new Intent(HomeActivity.this, CreatePostActivity.class));
            });
        }

        // Category Pills
        if (pillAll != null) pillAll.setOnClickListener(v -> selectCategory("Tất cả", pillAll));
        if (pillLiterature != null) pillLiterature.setOnClickListener(v -> selectCategory("Văn học", pillLiterature));
        if (pillEconomics != null) pillEconomics.setOnClickListener(v -> selectCategory("Kinh tế & Kỹ năng", pillEconomics));
        if (pillTextbook != null) pillTextbook.setOnClickListener(v -> selectCategory("Giáo trình", pillTextbook));
        if (pillOther != null) pillOther.setOnClickListener(v -> selectCategory("Khác", pillOther));

        // Filter Button
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilterDialog());
        }

        // Bottom Navigation Tabs & Brand Logo
        View.OnClickListener resetToDashboardTop = v -> {
            if (etSearchBook != null && etSearchBook.getText().length() > 0) {
                etSearchBook.setText("");
            }
            filterCondition = "Tất cả";
            filterSharingType = "Tất cả";
            filterFavoriteMode = "Tất cả sách";
            filterSortOrder = "Mới nhất";
            selectCategory("Tất cả", pillAll);
            if (mainScrollView != null) {
                mainScrollView.smoothScrollTo(0, 0);
            }
        };
        if (layoutBrandLogo != null) layoutBrandLogo.setOnClickListener(resetToDashboardTop);
        if (ivBrandLogoIcon != null) ivBrandLogoIcon.setOnClickListener(resetToDashboardTop);

        if (btnNavHome != null) {
            btnNavHome.setOnClickListener(v -> {
                if (mainScrollView != null) {
                    mainScrollView.smoothScrollTo(0, 0);
                }
            });
        }

        if (btnNavSearch != null) {
            btnNavSearch.setOnClickListener(v -> {
                if (mainScrollView != null && etSearchBook != null) {
                    mainScrollView.smoothScrollTo(0, etSearchBook.getTop());
                    etSearchBook.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(etSearchBook, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            });
        }

        if (btnNavRequests != null) {
            btnNavRequests.setOnClickListener(v -> {
                if (!checkGuestLogin()) return;
                startActivity(new Intent(HomeActivity.this, ManageRequestsActivity.class));
            });
        }

        // Pagination Buttons
        if (btnPrevPage != null) {
            btnPrevPage.setOnClickListener(v -> {
                if (currentPage > 1) {
                    currentPage--;
                    updatePagination();
                    if (mainScrollView != null) mainScrollView.smoothScrollTo(0, rvBooks.getTop());
                }
            });
        }

        if (btnNextPage != null) {
            btnNextPage.setOnClickListener(v -> {
                if (currentPage < totalPages) {
                    currentPage++;
                    updatePagination();
                    if (mainScrollView != null) mainScrollView.smoothScrollTo(0, rvBooks.getTop());
                }
            });
        }
    }

    private void setupPressEffects() {
        ViewPressEffectHelper.applyPressEffect(
                btnBackToAdminDashboard, btnNotifications, btnGoToProfile, btnClearSearch,
                pillAll, pillLiterature, pillEconomics, pillTextbook, pillOther,
                btnFilter, btnPrevPage, btnNextPage,
                btnNavHome, btnNavSearch, fabAddBook, btnNavRequests, btnNavProfile
        );
    }

    private void selectCategory(String category, TextView selectedPill) {
        selectedCategory = category;
        TextView[] allPills = {pillAll, pillLiterature, pillEconomics, pillTextbook, pillOther};
        String[] baseTexts = {"Tất cả", "Văn học", "Kinh tế & Kỹ năng", "Giáo trình", "Khác"};

        for (int i = 0; i < allPills.length; i++) {
            TextView pill = allPills[i];
            if (pill == null) continue;
            if (pill == selectedPill) {
                pill.setBackgroundResource(R.drawable.bg_pill_active);
                pill.setTextColor(getResources().getColor(R.color.on_primary));
                pill.setText("✓ " + baseTexts[i]);
                pill.setTypeface(null, Typeface.BOLD);
            } else {
                pill.setBackgroundResource(R.drawable.bg_pill_inactive);
                pill.setTextColor(getResources().getColor(R.color.on_surface_variant));
                pill.setText(baseTexts[i]);
                pill.setTypeface(null, Typeface.NORMAL);
            }
        }
        currentPage = 1;
        applyFilters();
    }

    private void showNotificationsDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_notifications, null);
        dialog.setContentView(sheetView);

        TextView tvHeaderBadge = sheetView.findViewById(R.id.tvHeaderNotificationCount);
        TextView btnMarkAllRead = sheetView.findViewById(R.id.btnMarkAllRead);
        ImageView btnClose = sheetView.findViewById(R.id.btnCloseNotifications);

        if (btnMarkAllRead != null) {
            btnMarkAllRead.setOnClickListener(v -> {
                FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                if (u != null) {
                    NotificationRepository.getInstance().markAllAsRead(u.getUid());
                    Toast.makeText(HomeActivity.this, "Đã đánh dấu tất cả thông báo là đã đọc", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
        }
        View layoutEmpty = sheetView.findViewById(R.id.layoutEmptyNotifications);
        View layoutContent = sheetView.findViewById(R.id.layoutNotificationsContent);
        TextView tvSummary = sheetView.findViewById(R.id.tvNotificationSummary);
        RecyclerView rvNotifications = sheetView.findViewById(R.id.rvNotifications);
        TextView btnViewRequests = sheetView.findViewById(R.id.btnViewRequestsFromNotification);
        View scrollContainer = sheetView.findViewById(R.id.scrollNotificationsList);

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        List<NotificationItem> notifications = buildNotificationList();
        int totalActiveNotifs = pendingReceivedCount + acceptedSentCount;

        if (notifications.isEmpty()) {
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
            if (layoutContent != null) layoutContent.setVisibility(View.GONE);
            if (tvHeaderBadge != null) tvHeaderBadge.setVisibility(View.GONE);
        } else {
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
            if (layoutContent != null) layoutContent.setVisibility(View.VISIBLE);

            if (totalActiveNotifs > 0 && tvHeaderBadge != null) {
                tvHeaderBadge.setText(totalActiveNotifs + " mới");
                tvHeaderBadge.setVisibility(View.VISIBLE);
            } else if (tvHeaderBadge != null) {
                tvHeaderBadge.setText(notifications.size() + " tin");
                tvHeaderBadge.setVisibility(View.VISIBLE);
            }

            if (tvSummary != null) {
                if (pendingReceivedCount > 0 && acceptedSentCount > 0) {
                    tvSummary.setText("Bạn có " + pendingReceivedCount + " yêu cầu chờ phản hồi và " + acceptedSentCount + " yêu cầu đã được chấp nhận:");
                } else if (pendingReceivedCount > 0) {
                    tvSummary.setText("Bạn có " + pendingReceivedCount + " yêu cầu nhận sách đang chờ phản hồi:");
                } else if (acceptedSentCount > 0) {
                    tvSummary.setText("Bạn có " + acceptedSentCount + " yêu cầu xin sách đã được chủ sách đồng ý:");
                } else {
                    tvSummary.setText("Danh sách thông báo hoạt động của bạn:");
                }
            }

            // Giới hạn chiều cao danh sách thông báo tối đa 48% màn hình để trải nghiệm cuộn thuận tiện
            if (scrollContainer != null) {
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                android.view.ViewGroup.LayoutParams params = scrollContainer.getLayoutParams();
                if (params != null) {
                    params.height = (int) (screenHeight * 0.48);
                    scrollContainer.setLayoutParams(params);
                }
            }

            NotificationAdapter adapter = new NotificationAdapter(item -> {
                dialog.dismiss();
                // TC-NOTIF: Đánh dấu đã đọc trên Firestore khi người dùng bấm vào thông báo
                if (item.getId() != null) {
                    NotificationRepository.getInstance().markAsRead(item.getId());
                }

                if ("BOOK_DETAIL".equalsIgnoreCase(item.getTargetScreen()) && item.getBookId() != null) {
                    Intent bookIntent = new Intent(HomeActivity.this, BookDetailActivity.class);
                    bookIntent.putExtra("bookId", item.getBookId());
                    startActivity(bookIntent);
                } else {
                    Intent reqIntent = new Intent(HomeActivity.this, ManageRequestsActivity.class);
                    reqIntent.putExtra("target_tab", item.getTargetTab());
                    reqIntent.putExtra("show_sent", "sent".equalsIgnoreCase(item.getTargetTab()));
                    if (item.getRequestId() != null) {
                        reqIntent.putExtra("highlight_request_id", item.getRequestId());
                    }
                    startActivity(reqIntent);
                }
            });

            adapter.setItems(notifications);
            if (rvNotifications != null) {
                rvNotifications.setAdapter(adapter);
            }

            if (btnViewRequests != null) {
                btnViewRequests.setText("Mở Quản lý yêu cầu (" + notifications.size() + ")");
                btnViewRequests.setOnClickListener(v -> {
                    dialog.dismiss();
                    startActivity(new Intent(HomeActivity.this, ManageRequestsActivity.class));
                });
            }
        }

        ViewPressEffectHelper.applyPressEffect(btnClose, btnViewRequests, btnMarkAllRead);
        dialog.show();
    }

    private void showFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_filter_books, null);
        dialog.setContentView(sheetView);

        ImageView btnClose = sheetView.findViewById(R.id.btnCloseFilter);
        TextView chipCondAll = sheetView.findViewById(R.id.chipCondAll);
        TextView chipCondNew = sheetView.findViewById(R.id.chipCondNew);
        TextView chipCondUsed = sheetView.findViewById(R.id.chipCondUsed);

        TextView chipTypeAll = sheetView.findViewById(R.id.chipTypeAll);
        TextView chipTypeFree = sheetView.findViewById(R.id.chipTypeFree);
        TextView chipTypeExchange = sheetView.findViewById(R.id.chipTypeExchange);

        TextView chipFavAll = sheetView.findViewById(R.id.chipFavAll);
        TextView chipFavOnly = sheetView.findViewById(R.id.chipFavOnly);

        TextView chipSortNewest = sheetView.findViewById(R.id.chipSortNewest);
        TextView chipSortOldest = sheetView.findViewById(R.id.chipSortOldest);
        TextView chipSortTitle = sheetView.findViewById(R.id.chipSortTitle);

        TextView btnReset = sheetView.findViewById(R.id.btnResetFilter);
        TextView btnApply = sheetView.findViewById(R.id.btnApplyFilter);

        final String[] tempCondition = {filterCondition};
        final String[] tempType = {filterSharingType};
        final String[] tempFav = {filterFavoriteMode};
        final String[] tempSort = {filterSortOrder};

        // Initialize chips UI according to current filter
        updateChipGroupState(tempCondition[0], "Tất cả", chipCondAll, "Còn mới / Rất mới", chipCondNew, "Đã qua sử dụng", chipCondUsed);
        updateChipGroupState(tempType[0], "Tất cả", chipTypeAll, "Tặng miễn phí", chipTypeFree, "Trao đổi", chipTypeExchange);
        if ("❤️ Danh sách yêu thích".equals(tempFav[0])) {
            setChipActive(chipFavOnly, chipFavAll);
        } else {
            setChipActive(chipFavAll, chipFavOnly);
        }
        updateChipGroupState(tempSort[0], "Mới nhất", chipSortNewest, "Cũ nhất", chipSortOldest, "Tên sách (A-Z)", chipSortTitle);

        // Chip click listeners
        if (chipCondAll != null) chipCondAll.setOnClickListener(v -> {
            tempCondition[0] = "Tất cả";
            setChipActive(chipCondAll, chipCondNew, chipCondUsed);
        });
        if (chipCondNew != null) chipCondNew.setOnClickListener(v -> {
            tempCondition[0] = "Còn mới / Rất mới";
            setChipActive(chipCondNew, chipCondAll, chipCondUsed);
        });
        if (chipCondUsed != null) chipCondUsed.setOnClickListener(v -> {
            tempCondition[0] = "Đã qua sử dụng";
            setChipActive(chipCondUsed, chipCondAll, chipCondNew);
        });

        if (chipTypeAll != null) chipTypeAll.setOnClickListener(v -> {
            tempType[0] = "Tất cả";
            setChipActive(chipTypeAll, chipTypeFree, chipTypeExchange);
        });
        if (chipTypeFree != null) chipTypeFree.setOnClickListener(v -> {
            tempType[0] = "Tặng miễn phí";
            setChipActive(chipTypeFree, chipTypeAll, chipTypeExchange);
        });
        if (chipTypeExchange != null) chipTypeExchange.setOnClickListener(v -> {
            tempType[0] = "Trao đổi";
            setChipActive(chipTypeExchange, chipTypeAll, chipTypeFree);
        });

        if (chipFavAll != null) chipFavAll.setOnClickListener(v -> {
            tempFav[0] = "Tất cả sách";
            setChipActive(chipFavAll, chipFavOnly);
        });
        if (chipFavOnly != null) chipFavOnly.setOnClickListener(v -> {
            tempFav[0] = "❤️ Danh sách yêu thích";
            setChipActive(chipFavOnly, chipFavAll);
        });

        if (chipSortNewest != null) chipSortNewest.setOnClickListener(v -> {
            tempSort[0] = "Mới nhất";
            setChipActive(chipSortNewest, chipSortOldest, chipSortTitle);
        });
        if (chipSortOldest != null) chipSortOldest.setOnClickListener(v -> {
            tempSort[0] = "Cũ nhất";
            setChipActive(chipSortOldest, chipSortNewest, chipSortTitle);
        });
        if (chipSortTitle != null) chipSortTitle.setOnClickListener(v -> {
            tempSort[0] = "Tên sách (A-Z)";
            setChipActive(chipSortTitle, chipSortNewest, chipSortOldest);
        });

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                tempCondition[0] = "Tất cả";
                tempType[0] = "Tất cả";
                tempFav[0] = "Tất cả sách";
                tempSort[0] = "Mới nhất";
                setChipActive(chipCondAll, chipCondNew, chipCondUsed);
                setChipActive(chipTypeAll, chipTypeFree, chipTypeExchange);
                setChipActive(chipFavAll, chipFavOnly);
                setChipActive(chipSortNewest, chipSortOldest, chipSortTitle);
            });
        }

        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                filterCondition = tempCondition[0];
                filterSharingType = tempType[0];
                filterFavoriteMode = tempFav[0];
                filterSortOrder = tempSort[0];
                dialog.dismiss();
                currentPage = 1;
                applyFilters();
            });
        }

        ViewPressEffectHelper.applyPressEffect(
                btnClose, btnReset, btnApply,
                chipCondAll, chipCondNew, chipCondUsed,
                chipTypeAll, chipTypeFree, chipTypeExchange,
                chipFavAll, chipFavOnly,
                chipSortNewest, chipSortOldest, chipSortTitle
        );

        dialog.show();
    }

    private void updateChipGroupState(String currentVal, String val1, TextView chip1, String val2, TextView chip2, String val3, TextView chip3) {
        if (val2.equals(currentVal)) {
            setChipActive(chip2, chip1, chip3);
        } else if (val3.equals(currentVal)) {
            setChipActive(chip3, chip1, chip2);
        } else {
            setChipActive(chip1, chip2, chip3);
        }
    }

    private void setChipActive(TextView activeChip, TextView... otherChips) {
        if (activeChip != null) {
            activeChip.setBackgroundResource(R.drawable.bg_pill_active);
            activeChip.setTextColor(getResources().getColor(R.color.on_primary));
            activeChip.setTypeface(null, Typeface.BOLD);
        }
        for (TextView other : otherChips) {
            if (other != null) {
                other.setBackgroundResource(R.drawable.bg_pill_inactive);
                other.setTextColor(getResources().getColor(R.color.on_surface_variant));
                other.setTypeface(null, Typeface.NORMAL);
            }
        }
    }

    private void applyFilters() {
        if (allBooksList == null) return;

        currentFilteredList = new ArrayList<>();
        String searchQuery = etSearchBook != null ? etSearchBook.getText().toString().trim().toLowerCase() : "";
        FirebaseUser currentUserForFilter = FirebaseAuth.getInstance().getCurrentUser();
        String currentUidForFilter = currentUserForFilter != null ? currentUserForFilter.getUid() : null;

        for (Book b : allBooksList) {
            // 1. Check status hidden, given, or exchanged
            String status = b.getStatus();
            if ("hidden".equalsIgnoreCase(status) || "given".equalsIgnoreCase(status) || "exchanged".equalsIgnoreCase(status)) {
                continue;
            }

            // 1b. TC-ADMIN-06: Sách chờ Admin duyệt chỉ hiển thị cho chính người đăng (nhãn "Chờ duyệt")
            if (Book.isPendingReview(b)
                    && (currentUidForFilter == null || !currentUidForFilter.equals(b.getOwnerId()))) {
                continue;
            }

            // 2. Category Filter
            if (!"Tất cả".equals(selectedCategory)) {
                String bookCategory = b.getCategory() != null ? b.getCategory().toLowerCase() : "";
                if ("Văn học".equals(selectedCategory)) {
                    if (!bookCategory.contains("văn học") && !bookCategory.contains("tiểu thuyết") && !bookCategory.contains("truyện")) {
                        continue;
                    }
                } else if ("Kinh tế & Kỹ năng".equals(selectedCategory)) {
                    if (!bookCategory.contains("kinh tế") && !bookCategory.contains("kỹ năng")) {
                        continue;
                    }
                } else if ("Giáo trình".equals(selectedCategory)) {
                    if (!bookCategory.contains("giáo trình") && !bookCategory.contains("học thuật")) {
                        continue;
                    }
                } else if ("Khác".equals(selectedCategory)) {
                    if (bookCategory.contains("văn học") || bookCategory.contains("kinh tế")
                            || bookCategory.contains("kỹ năng") || bookCategory.contains("giáo trình")) {
                        continue;
                    }
                }
            }

            // 3. Condition Filter
            if (!"Tất cả".equals(filterCondition)) {
                String bookCond = b.getCondition() != null ? b.getCondition().toLowerCase() : "";
                if ("Còn mới / Rất mới".equals(filterCondition)) {
                    if (!bookCond.contains("mới") && !bookCond.contains("new") && !bookCond.contains("99") && !bookCond.contains("98")) {
                        continue;
                    }
                } else if ("Đã qua sử dụng".equals(filterCondition)) {
                    if (bookCond.contains("mới") && !bookCond.contains("cũ") && !bookCond.contains("đã qua")) {
                        continue;
                    }
                }
            }

            // 4. Sharing Type Filter
            if (!"Tất cả".equals(filterSharingType)) {
                String bookStatus = b.getStatus() != null ? b.getStatus().toLowerCase() : "available";
                if ("Tặng miễn phí".equals(filterSharingType)) {
                    if (!"available".equals(bookStatus) && !bookStatus.contains("miễn phí")) {
                        continue;
                    }
                } else if ("Trao đổi".equals(filterSharingType)) {
                    if (!"exchanging".equals(bookStatus) && !bookStatus.contains("trao đổi")) {
                        continue;
                    }
                }
            }

            // 4b. Favorite Books Filter
            if ("❤️ Danh sách yêu thích".equals(filterFavoriteMode)) {
                if (b.getId() == null || !currentFavoriteBookIds.contains(b.getId())) {
                    continue;
                }
            }

            // 5. Search Text Query (Title, Author, Category, Description)
            if (!searchQuery.isEmpty()) {
                String title = b.getTitle() != null ? b.getTitle().toLowerCase() : "";
                String author = b.getAuthor() != null ? b.getAuthor().toLowerCase() : "";
                String category = b.getCategory() != null ? b.getCategory().toLowerCase() : "";
                String desc = b.getDescription() != null ? b.getDescription().toLowerCase() : "";

                if (!title.contains(searchQuery) && !author.contains(searchQuery)
                        && !category.contains(searchQuery) && !desc.contains(searchQuery)) {
                    continue;
                }
            }

            currentFilteredList.add(b);
        }

        // 6. Sorting
        if ("Cũ nhất".equals(filterSortOrder)) {
            currentFilteredList.sort((b1, b2) -> Long.compare(b1.getCreatedAt(), b2.getCreatedAt()));
        } else if ("Tên sách (A-Z)".equals(filterSortOrder)) {
            currentFilteredList.sort((b1, b2) -> {
                String t1 = b1.getTitle() != null ? b1.getTitle() : "";
                String t2 = b2.getTitle() != null ? b2.getTitle() : "";
                return t1.compareToIgnoreCase(t2);
            });
        } else {
            // Default: Mới nhất
            currentFilteredList.sort((b1, b2) -> Long.compare(b2.getCreatedAt(), b1.getCreatedAt()));
        }

        // Update count badge
        if (tvBookCountBadge != null) {
            tvBookCountBadge.setText(currentFilteredList.size() + " cuốn");
        }

        // Calculate total pages
        totalPages = (int) Math.ceil((double) currentFilteredList.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        updatePagination();
    }

    private void updatePagination() {
        if (currentFilteredList.isEmpty()) {
            tvEmptyBooks.setVisibility(View.VISIBLE);
            rvBooks.setVisibility(View.GONE);
            findViewById(R.id.layoutPagination).setVisibility(View.GONE);
            return;
        }

        tvEmptyBooks.setVisibility(View.GONE);
        rvBooks.setVisibility(View.VISIBLE);
        findViewById(R.id.layoutPagination).setVisibility(View.VISIBLE);

        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, currentFilteredList.size());

        List<Book> pageItems = currentFilteredList.subList(startIndex, endIndex);
        bookAdapter.update(pageItems);

        tvPageIndicator.setText(currentPage + " / " + totalPages);

        btnPrevPage.setVisibility(currentPage > 1 ? View.VISIBLE : View.INVISIBLE);
        btnNextPage.setVisibility(currentPage < totalPages ? View.VISIBLE : View.INVISIBLE);
    }
}
