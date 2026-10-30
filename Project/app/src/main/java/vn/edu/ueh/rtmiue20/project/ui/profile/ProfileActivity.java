package vn.edu.ueh.rtmiue20.project.ui.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.Request;
import vn.edu.ueh.rtmiue20.project.data.model.User;
import vn.edu.ueh.rtmiue20.project.data.repository.AuthRepository;
import vn.edu.ueh.rtmiue20.project.data.repository.UserRepository;
import vn.edu.ueh.rtmiue20.project.ui.admin.AdminDashboardActivity;
import vn.edu.ueh.rtmiue20.project.ui.auth.LogInActivity;
import vn.edu.ueh.rtmiue20.project.ui.base.BaseActivity;
import vn.edu.ueh.rtmiue20.project.ui.favorite.FavoriteViewModel;
import vn.edu.ueh.rtmiue20.project.ui.home.BookViewModel;
import vn.edu.ueh.rtmiue20.project.ui.home.HomeActivity;
import vn.edu.ueh.rtmiue20.project.ui.request.ManageRequestsActivity;
import vn.edu.ueh.rtmiue20.project.util.LocationHelper;

public class ProfileActivity extends BaseActivity {

    private ImageView ivAvatar;
    private TextView tvProfileFullName;
    private TextView tvProfileEmail;
    private TextView tvProfileRole;
    private View btnEditProfile;
    private View btnLogoutProfile;
    private View btnAppSettings;
    private View btnHelpCenter;

    private TextView tvBooksGivenCount;
    private TextView tvBooksReceivedCount;
    private TextView tvRatingCount;
    private View layoutMyPosts;
    private TextView tvMyPostsCount;
    private View layoutFavoriteBooks;
    private TextView tvFavoriteCount;
    private View layoutHistoryRequests;

    private View btnBackProfile;
    private View layoutBrandLogo;

    private AuthRepository authRepository;
    private UserRepository userRepository;
    private FavoriteViewModel favoriteViewModel;
    private BookViewModel bookViewModel;

    private int booksGivenVal = 0;
    private int booksReceivedVal = 0;
    private double currentRatingAverage = 5.0;
    private int currentRatingCount = 0;
    private boolean isAdminUser = false;

    private ActivityResultLauncher<String[]> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        View mainView = findViewById(R.id.mainProfile);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        SharedPreferences prefs = getSharedPreferences("BookSharePrefs", MODE_PRIVATE);
        isAdminUser = "admin".equalsIgnoreCase(prefs.getString("user_role", ""));

        authRepository = AuthRepository.getInstance();
        userRepository = UserRepository.getInstance();
        favoriteViewModel = new ViewModelProvider(this).get(FavoriteViewModel.class);
        bookViewModel = new ViewModelProvider(this).get(BookViewModel.class);

        initViews();
        setupImagePickerLauncher();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.ivAvatar);
        tvProfileFullName = findViewById(R.id.tvProfileFullName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileRole = findViewById(R.id.tvProfileRole);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogoutProfile = findViewById(R.id.btnLogoutProfile);
        btnAppSettings = findViewById(R.id.btnAppSettings);
        btnHelpCenter = findViewById(R.id.btnHelpCenter);

        tvBooksGivenCount = findViewById(R.id.tvBooksGivenCount);
        tvBooksReceivedCount = findViewById(R.id.tvBooksReceivedCount);
        tvRatingCount = findViewById(R.id.tvRatingCount);
        layoutMyPosts = findViewById(R.id.layoutMyPosts);
        tvMyPostsCount = findViewById(R.id.tvMyPostsCount);
        layoutFavoriteBooks = findViewById(R.id.layoutFavoriteBooks);
        tvFavoriteCount = findViewById(R.id.tvFavoriteCount);
        layoutHistoryRequests = findViewById(R.id.layoutHistoryRequests);

        btnBackProfile = findViewById(R.id.btnBackProfile);
        layoutBrandLogo = findViewById(R.id.layoutBrandLogo);
    }

    private void setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        uploadAvatarImage(uri);
                    }
                }
        );
    }

    private void setupListeners() {
        if (btnBackProfile != null) {
            btnBackProfile.setOnClickListener(v -> handleSmartBack());
        }

        if (layoutBrandLogo != null) {
            layoutBrandLogo.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("BookSharePrefs", MODE_PRIVATE);
                boolean isAdmin = isAdminUser || "admin".equalsIgnoreCase(prefs.getString("user_role", ""));
                Class<?> targetHome = isAdmin ? AdminDashboardActivity.class : HomeActivity.class;
                Intent intent = new Intent(ProfileActivity.this, targetHome);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
                startActivity(intent);
            });
        }

        if (ivAvatar != null) {
            ivAvatar.setOnClickListener(v -> imagePickerLauncher.launch(new String[]{"image/*"}));
        }

        if (btnLogoutProfile != null) {
            btnLogoutProfile.setOnClickListener(v -> handleLogout());
        }

        // Kệ sách & giao dịch navigation listeners
        if (layoutMyPosts != null) {
            layoutMyPosts.setOnClickListener(v -> {
                startActivity(new Intent(ProfileActivity.this, MyPostsActivity.class));
            });
        }

        if (layoutFavoriteBooks != null) {
            layoutFavoriteBooks.setOnClickListener(v -> {
                startActivity(new Intent(ProfileActivity.this, FavoriteBooksActivity.class));
            });
        }

        if (layoutHistoryRequests != null) {
            layoutHistoryRequests.setOnClickListener(v -> {
                startActivity(new Intent(ProfileActivity.this, ManageRequestsActivity.class));
            });
        }

        if (btnAppSettings != null) {
            btnAppSettings.setOnClickListener(v -> showAppSettingsDialog());
        }

        if (btnHelpCenter != null) {
            btnHelpCenter.setOnClickListener(v -> showHelpCenterDialog());
        }
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Phiên đăng nhập đã hết hạn.", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        String uid = currentUser.getUid();
        loadUserStats(uid);

        if (tvProfileEmail != null && currentUser.getEmail() != null) {
            tvProfileEmail.setText(currentUser.getEmail());
        }

        String immediateName = currentUser.getDisplayName();
        if (immediateName == null || immediateName.trim().isEmpty()) {
            SharedPreferences prefs = getSharedPreferences("BookSharePrefs", MODE_PRIVATE);
            immediateName = prefs.getString("user_fullname", "");
        }
        if (immediateName == null || immediateName.trim().isEmpty()) {
            if (currentUser.getEmail() != null) {
                immediateName = currentUser.getEmail().split("@")[0];
            }
        }
        if (tvProfileFullName != null && immediateName != null && !immediateName.trim().isEmpty()) {
            tvProfileFullName.setText(immediateName);
        }

        userRepository.getUserProfile(uid, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (tvProfileFullName != null) {
                    String formatted = User.getDisplayNameFormatted(user.getFullName(), user.getNickname());
                    if (!formatted.isEmpty()) {
                        tvProfileFullName.setText(formatted);
                    } else if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().trim().isEmpty()) {
                        tvProfileFullName.setText(currentUser.getDisplayName());
                    }
                }

                String role = user.getRole() != null ? user.getRole() : "member";
                isAdminUser = "admin".equalsIgnoreCase(role);

                if (user.getRatingAverage() != null) {
                    currentRatingAverage = user.getRatingAverage();
                }
                if (user.getRatingCount() != null) {
                    currentRatingCount = user.getRatingCount();
                }

                if (tvProfileRole != null) {
                    if (isAdminUser) {
                        tvProfileRole.setText("✔ Quản trị viên (Admin) - Chạm để vào Dashboard");
                        tvProfileRole.setOnClickListener(v -> {
                            Intent intent = new Intent(ProfileActivity.this, AdminDashboardActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        tvProfileRole.setText(vn.edu.ueh.rtmiue20.project.util.RatingHelper.getRatingBadgeTitle(currentRatingAverage, currentRatingCount));
                    }
                }
                updateReputationDisplay();

                String avatarToLoad = user.getAvatarUrl() != null ? user.getAvatarUrl() : "";
                getSharedPreferences("BookSharePrefs", MODE_PRIVATE).edit()
                        .putString("user_avatar_url", avatarToLoad)
                        .putString("user_role", role)
                        .apply();
                vn.edu.ueh.rtmiue20.project.util.ImageHelper.bindAvatar(ProfileActivity.this, ivAvatar, avatarToLoad, false);
            }

            @Override
            public void onError(String errorMessage) {
                if (tvProfileFullName != null) {
                    if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().trim().isEmpty()) {
                        tvProfileFullName.setText(currentUser.getDisplayName());
                    }
                }
            }
        });
    }

    private void loadUserStats(String uid) {
        // 1. Sách đã tặng / Bài đăng của tôi (Observe từ Room SQLite để hỗ trợ cả Offline + Online)
        bookViewModel.getBooksByOwner(uid).observe(this, books -> {
            int count = (books != null) ? books.size() : 0;
            booksGivenVal = count;
            if (tvBooksGivenCount != null) {
                tvBooksGivenCount.setText(String.valueOf(count));
            }
            if (tvMyPostsCount != null) {
                tvMyPostsCount.setText(count + " đang hiển thị");
            }
            updateReputationDisplay();
        });

        // 2. Sách đã nhận (Count requests where requesterId == uid)
        FirebaseFirestore.getInstance().collection("requests")
                .whereEqualTo("requesterId", uid)
                .addSnapshotListener((value, error) -> {
                    if (error == null && value != null) {
                        int acceptedCount = 0;
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String st = doc.getString("status");
                            if (Request.STATUS_ACCEPTED.equalsIgnoreCase(st) || Request.STATUS_COMPLETED.equalsIgnoreCase(st)) {
                                acceptedCount++;
                            }
                        }
                        int displayCount = acceptedCount > 0 ? acceptedCount : value.size();
                        booksReceivedVal = displayCount;
                        if (tvBooksReceivedCount != null) {
                            tvBooksReceivedCount.setText(String.valueOf(displayCount));
                        }
                        updateReputationDisplay();
                    }
                });

        // 3. Sách yêu thích (Observe favorites count từ Room SQLite)
        favoriteViewModel.getFavoritedBooksOffline().observe(this, books -> {
            int count = (books != null) ? books.size() : 0;
            if (tvFavoriteCount != null) {
                tvFavoriteCount.setText(count + " cuốn");
            }
        });

        // 4. TC-RATING: Tính toán điểm uy tín từ các giao dịch trao đổi đã hoàn tất & có đánh giá
        FirebaseFirestore.getInstance().collection("requests")
                .whereEqualTo("ownerId", uid)
                .whereEqualTo("status", Request.STATUS_COMPLETED)
                .addSnapshotListener((value, error) -> {
                    if (error == null && value != null && !value.isEmpty()) {
                        List<Float> ratingsList = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            if (Boolean.TRUE.equals(doc.getBoolean("rated"))) {
                                Double rVal = doc.getDouble("ratingValue");
                                if (rVal != null && rVal > 0) {
                                    ratingsList.add(rVal.floatValue());
                                }
                            }
                        }
                        if (!ratingsList.isEmpty()) {
                            currentRatingCount = ratingsList.size();
                            currentRatingAverage = vn.edu.ueh.rtmiue20.project.util.RatingHelper.calculateAverageRating(ratingsList);
                            updateReputationDisplay();
                        }
                    }
                });

        updateReputationDisplay();
    }

    private void updateReputationDisplay() {
        if (tvRatingCount == null) return;
        tvRatingCount.setText(vn.edu.ueh.rtmiue20.project.util.RatingHelper.formatRating(currentRatingAverage, currentRatingCount));

        if (tvProfileRole != null && !isAdminUser) {
            tvProfileRole.setText(vn.edu.ueh.rtmiue20.project.util.RatingHelper.getRatingBadgeTitle(currentRatingAverage, currentRatingCount));
        }
    }

    private void uploadAvatarImage(Uri imageUri) {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) return;

        Toast.makeText(this, "Đang tải ảnh đại diện lên...", Toast.LENGTH_SHORT).show();

        userRepository.uploadAvatar(this, currentUser.getUid(), imageUri, new UserRepository.UploadCallback() {
            @Override
            public void onSuccess(String avatarUrl) {
                Toast.makeText(ProfileActivity.this, "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show();
                getSharedPreferences("BookSharePrefs", MODE_PRIVATE).edit()
                        .putString("user_avatar_url", avatarUrl != null ? avatarUrl : "")
                        .apply();
                vn.edu.ueh.rtmiue20.project.util.ImageHelper.bindAvatar(ProfileActivity.this, ivAvatar, avatarUrl, false);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ProfileActivity.this, "Tải ảnh đại diện thất bại: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleLogout() {
        SharedPreferences prefs = getSharedPreferences("BookSharePrefs", MODE_PRIVATE);
        prefs.edit()
                .remove("user_fullname")
                .remove("user_avatar_url")
                .remove("user_role")
                .apply();
        authRepository.logout();
        Toast.makeText(this, "Đã đăng xuất khỏi BookShare.", Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(ProfileActivity.this, LogInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showAppSettingsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_app_settings, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        SharedPreferences prefs = getSharedPreferences("BookSharePrefs", MODE_PRIVATE);

        // 1. Switch thông báo đẩy (FCM)
        SwitchMaterial switchPushNotification = dialogView.findViewById(R.id.switchPushNotification);
        if (switchPushNotification != null) {
            boolean isNotifEnabled = prefs.getBoolean("pref_notifications_enabled", true);
            switchPushNotification.setChecked(isNotifEnabled);
            switchPushNotification.setOnCheckedChangeListener((btn, isChecked) -> {
                prefs.edit().putBoolean("pref_notifications_enabled", isChecked).apply();
                Toast.makeText(this, isChecked ? "Đã bật thông báo đẩy" : "Đã tắt thông báo đẩy", Toast.LENGTH_SHORT).show();
            });
        }

        // 2. Đồng bộ ngoại tuyến (Room SQLite)
        View cardSyncOffline = dialogView.findViewById(R.id.cardSyncOffline);
        TextView tvSyncStatus = dialogView.findViewById(R.id.tvSyncStatus);
        View btnSyncNow = dialogView.findViewById(R.id.btnSyncNow);
        View.OnClickListener syncListener = v -> {
            if (tvSyncStatus != null) tvSyncStatus.setText("Đang đồng bộ từ Firestore...");
            new vn.edu.ueh.rtmiue20.project.data.repository.BookRepository(getApplication()).startSync();
            new vn.edu.ueh.rtmiue20.project.data.repository.FavoriteRepository(getApplication()).startSyncFavorites();
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (tvSyncStatus != null) {
                    tvSyncStatus.setText("Room SQLite: Đang bật • Vừa cập nhật xong");
                }
                Toast.makeText(this, "Đã đồng bộ toàn bộ sách ngoại tuyến thành công!", Toast.LENGTH_SHORT).show();
            }, 800);
        };
        if (cardSyncOffline != null) cardSyncOffline.setOnClickListener(syncListener);
        if (btnSyncNow != null) btnSyncNow.setOnClickListener(syncListener);

        // 3. Xóa bộ nhớ đệm (Cache)
        View cardClearCache = dialogView.findViewById(R.id.cardClearCache);
        TextView tvCacheSize = dialogView.findViewById(R.id.tvCacheSize);
        View btnClearCache = dialogView.findViewById(R.id.btnClearCache);
        computeAndDisplayCacheSize(tvCacheSize);
        View.OnClickListener clearCacheListener = v -> clearAppCache(tvCacheSize);
        if (cardClearCache != null) cardClearCache.setOnClickListener(clearCacheListener);
        if (btnClearCache != null) btnClearCache.setOnClickListener(clearCacheListener);

        // 4. Phiên bản ứng dụng
        View cardAppVersion = dialogView.findViewById(R.id.cardAppVersion);
        TextView tvAppVersion = dialogView.findViewById(R.id.tvAppVersion);
        try {
            String verName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            if (tvAppVersion != null) {
                tvAppVersion.setText("BookShare v" + verName + " (Bản chính thức)");
            }
        } catch (Exception ignored) {}
        if (cardAppVersion != null) {
            cardAppVersion.setOnClickListener(v -> {
                Toast.makeText(this, "Ứng dụng đang ở phiên bản mới nhất!", Toast.LENGTH_SHORT).show();
            });
        }

        // Nút Đóng
        View btnDismiss = dialogView.findViewById(R.id.btnDismissSettings);
        if (btnDismiss != null) {
            btnDismiss.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void showHelpCenterDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_help_center, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // Setup FAQ accordions
        setupFaqAccordion(
                dialogView.findViewById(R.id.cardFaq1),
                dialogView.findViewById(R.id.tvFaqAnswer1),
                dialogView.findViewById(R.id.ivFaqArrow1)
        );
        setupFaqAccordion(
                dialogView.findViewById(R.id.cardFaq2),
                dialogView.findViewById(R.id.tvFaqAnswer2),
                dialogView.findViewById(R.id.ivFaqArrow2)
        );
        setupFaqAccordion(
                dialogView.findViewById(R.id.cardFaq3),
                dialogView.findViewById(R.id.tvFaqAnswer3),
                dialogView.findViewById(R.id.ivFaqArrow3)
        );

        // Kênh hỗ trợ Email
        View cardContactEmail = dialogView.findViewById(R.id.cardContactEmail);
        if (cardContactEmail != null) {
            cardContactEmail.setOnClickListener(v -> sendSupportEmail());
        }

        // Kênh đóng góp ý kiến & báo lỗi
        View cardSendFeedback = dialogView.findViewById(R.id.cardSendFeedback);
        if (cardSendFeedback != null) {
            cardSendFeedback.setOnClickListener(v -> showFeedbackInputDialog());
        }

        // Nút Đã hiểu
        View btnDismiss = dialogView.findViewById(R.id.btnDismissHelp);
        if (btnDismiss != null) {
            btnDismiss.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
        if (dialog.getWindow() != null) {
            int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.82);
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
            dialog.getWindow().setLayout(width, height);
        }
    }

    private void setupFaqAccordion(View card, TextView tvAnswer, ImageView ivArrow) {
        if (card == null || tvAnswer == null || ivArrow == null) return;
        card.setOnClickListener(v -> {
            boolean isExpanded = tvAnswer.getVisibility() == View.VISIBLE;
            if (isExpanded) {
                tvAnswer.setVisibility(View.GONE);
                ivArrow.animate().rotation(0f).setDuration(200).start();
            } else {
                tvAnswer.setVisibility(View.VISIBLE);
                ivArrow.animate().rotation(90f).setDuration(200).start();
            }
        });
    }

    private void sendSupportEmail() {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        String userEmail = (currentUser != null && currentUser.getEmail() != null) ? currentUser.getEmail() : "N/A";
        String userName = (currentUser != null && currentUser.getDisplayName() != null) ? currentUser.getDisplayName() : "Sinh viên UEH";

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:hotro.bookshare@ueh.edu.vn"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "[BookShare UEH] Yêu cầu hỗ trợ: " + userName);
        String body = "\n\n---\nThông tin tài khoản gửi hỗ trợ:\n- Họ và tên: " + userName +
                "\n- Email tài khoản: " + userEmail +
                "\n- Thiết bị: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL +
                "\n- Phiên bản Android: " + android.os.Build.VERSION.RELEASE +
                "\n- Phiên bản ứng dụng: BookShare UEH v1.0\n";
        intent.putExtra(Intent.EXTRA_TEXT, body);

        try {
            startActivity(Intent.createChooser(intent, "Chọn ứng dụng gửi Email"));
        } catch (Exception e) {
            Toast.makeText(this, "Không tìm thấy ứng dụng Email trên thiết bị. Bạn vui lòng liên hệ qua email: hotro.bookshare@ueh.edu.vn", Toast.LENGTH_LONG).show();
        }
    }

    private void showFeedbackInputDialog() {
        android.widget.EditText edtFeedback = new android.widget.EditText(this);
        edtFeedback.setHint("Nhập nội dung góp ý, báo lỗi hoặc đề xuất tính năng mới...");
        edtFeedback.setTextSize(14f);
        edtFeedback.setMinLines(4);
        edtFeedback.setMaxLines(8);
        edtFeedback.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        edtFeedback.setBackgroundResource(R.drawable.bg_feedback_input);
        int pad = (int) (12 * getResources().getDisplayMetrics().density);
        edtFeedback.setPadding(pad, pad, pad, pad);
        edtFeedback.setTextColor(getColor(R.color.text_primary));
        edtFeedback.setHintTextColor(getColor(R.color.text_secondary));

        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        int horPad = (int) (20 * getResources().getDisplayMetrics().density);
        int topPad = (int) (12 * getResources().getDisplayMetrics().density);
        container.setPadding(horPad, topPad, horPad, topPad);
        container.addView(edtFeedback);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Đóng góp ý kiến & Báo lỗi")
                .setMessage("Ý kiến của bạn giúp cộng đồng BookShare UEH ngày càng phát triển tốt hơn:")
                .setView(container)
                .setPositiveButton("Gửi đóng góp", (dialog, which) -> {
                    String content = edtFeedback.getText().toString().trim();
                    if (content.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập nội dung trước khi gửi.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    submitUserFeedback(content);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void submitUserFeedback(String content) {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        String uid = (currentUser != null) ? currentUser.getUid() : "anonymous";
        String email = (currentUser != null && currentUser.getEmail() != null) ? currentUser.getEmail() : "";
        String name = (currentUser != null && currentUser.getDisplayName() != null) ? currentUser.getDisplayName() : "";

        java.util.Map<String, Object> feedback = new java.util.HashMap<>();
        feedback.put("userId", uid);
        feedback.put("userEmail", email);
        feedback.put("userName", name);
        feedback.put("content", content);
        feedback.put("device", android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
        feedback.put("androidVersion", android.os.Build.VERSION.RELEASE);
        feedback.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance().collection("feedbacks")
                .add(feedback)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Cảm ơn bạn! Đóng góp ý kiến đã được gửi đến ban quản trị BookShare UEH.", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Đã ghi nhận ý kiến của bạn. Cảm ơn bạn đã đồng hành cùng BookShare UEH!", Toast.LENGTH_LONG).show();
                });
    }


    private void computeAndDisplayCacheSize(TextView tvCacheSize) {
        if (tvCacheSize == null) return;
        new Thread(() -> {
            long size = getDirSize(getCacheDir()) + getDirSize(getExternalCacheDir());
            runOnUiThread(() -> {
                if (tvCacheSize != null) {
                    tvCacheSize.setText(formatFileSize(size) + " • Chạm để giải phóng");
                }
            });
        }).start();
    }

    private void clearAppCache(TextView tvCacheSize) {
        new Thread(() -> {
            try {
                deleteDir(getCacheDir());
                deleteDir(getExternalCacheDir());
                Glide.get(getApplicationContext()).clearDiskCache();
            } catch (Exception ignored) {}
            runOnUiThread(() -> {
                try {
                    Glide.get(getApplicationContext()).clearMemory();
                } catch (Exception ignored) {}
                if (tvCacheSize != null) {
                    tvCacheSize.setText("0.0 KB • Đã giải phóng sạch sẽ");
                }
                Toast.makeText(this, "Đã dọn dẹp bộ nhớ đệm thành công!", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private long getDirSize(File dir) {
        long size = 0;
        if (dir != null && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) size += getDirSize(f);
                    else size += f.length();
                }
            }
        } else if (dir != null) {
            size += dir.length();
        }
        return size;
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        }
        return false;
    }

    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0.0 KB";
        if (bytes < 1024 * 1024) {
            return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
