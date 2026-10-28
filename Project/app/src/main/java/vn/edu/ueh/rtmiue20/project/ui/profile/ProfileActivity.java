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

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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
    private ImageView ivHeaderAvatar;
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
        ivHeaderAvatar = findViewById(R.id.ivHeaderAvatar);
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
            btnAppSettings.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Cài đặt ứng dụng")
                        .setMessage("• Đồng bộ ngoại tuyến (Room SQLite): Đang bật\n" +
                                "• Thông báo đẩy (Firebase Cloud Messaging): Đang bật\n" +
                                "• Điểm hẹn mặc định: UEH Cơ sở B (" + LocationHelper.UEH_CAMPUS_B_ADDRESS + ")\n" +
                                "• Phiên bản: BookShare v2.4.1")
                        .setPositiveButton("Đóng", null)
                        .show();
            });
        }

        if (btnHelpCenter != null) {
            btnHelpCenter.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Trung tâm trợ giúp & Điều khoản")
                        .setMessage("Chào mừng bạn đến với cộng đồng BookShare UEH!\n\n" +
                                "1. Tinh thần chia sẻ: Tặng miễn phí hoặc trao đổi sách văn minh, tôn trọng tác quyền.\n" +
                                "2. Điểm hẹn an toàn: Khuyến khích giao nhận trực tiếp tại khuôn viên UEH Cơ sở B (279 Nguyễn Tri Phương, Q.10).\n" +
                                "3. Giữ gìn sách: Hãy trân trọng cuốn sách nhận được và tiếp tục lan tỏa cho bạn đọc tiếp theo.")
                        .setPositiveButton("Đã hiểu", null)
                        .show();
            });
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
                        tvProfileRole.setText("✔ Thành viên uy tín 5 sao");
                    }
                }

                String avatarToLoad = user.getAvatarUrl() != null ? user.getAvatarUrl() : "";
                getSharedPreferences("BookSharePrefs", MODE_PRIVATE).edit()
                        .putString("user_avatar_url", avatarToLoad)
                        .putString("user_role", role)
                        .apply();
                vn.edu.ueh.rtmiue20.project.util.ImageHelper.bindAvatar(ProfileActivity.this, ivAvatar, avatarToLoad, false);
                vn.edu.ueh.rtmiue20.project.util.ImageHelper.bindAvatar(ProfileActivity.this, ivHeaderAvatar, avatarToLoad, false);
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
                            if (Request.STATUS_ACCEPTED.equalsIgnoreCase(st)) {
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

        updateReputationDisplay();
    }

    private void updateReputationDisplay() {
        if (tvRatingCount == null) return;
        int totalActivities = booksGivenVal + booksReceivedVal;
        if (totalActivities > 0) {
            tvRatingCount.setText("5.0 (" + totalActivities + ")");
        } else {
            tvRatingCount.setText("5.0 ★");
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
                vn.edu.ueh.rtmiue20.project.util.ImageHelper.bindAvatar(ProfileActivity.this, ivHeaderAvatar, avatarUrl, false);
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
}
