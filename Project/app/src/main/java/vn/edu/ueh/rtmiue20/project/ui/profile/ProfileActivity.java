package vn.edu.ueh.rtmiue20.project.ui.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.User;
import vn.edu.ueh.rtmiue20.project.data.repository.AuthRepository;
import vn.edu.ueh.rtmiue20.project.data.repository.UserRepository;
import vn.edu.ueh.rtmiue20.project.ui.admin.AdminDashboardActivity;
import vn.edu.ueh.rtmiue20.project.ui.auth.LogInActivity;
import vn.edu.ueh.rtmiue20.project.ui.base.BaseActivity;
import vn.edu.ueh.rtmiue20.project.ui.favorite.FavoriteViewModel;
import vn.edu.ueh.rtmiue20.project.ui.home.HomeActivity;
import vn.edu.ueh.rtmiue20.project.ui.request.ManageRequestsActivity;

public class ProfileActivity extends BaseActivity {

    private ImageView ivAvatar;
    private TextView tvProfileFullName;
    private TextView tvProfileEmail;
    private TextView tvProfileRole;
    private View btnEditProfile;
    private View btnLogoutProfile;

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

        authRepository = AuthRepository.getInstance();
        userRepository = UserRepository.getInstance();
        favoriteViewModel = new ViewModelProvider(this).get(FavoriteViewModel.class);

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
                Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
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
            android.content.SharedPreferences prefs = getSharedPreferences("BookSharePrefs", MODE_PRIVATE);
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

                if (tvProfileRole != null) {
                    if ("admin".equalsIgnoreCase(user.getRole())) {
                        tvProfileRole.setText("✔ Quản trị viên (Admin) - Chạm để vào Dashboard");
                        tvProfileRole.setOnClickListener(v ->
                                startActivity(new Intent(ProfileActivity.this, AdminDashboardActivity.class))
                        );
                    } else {
                        tvProfileRole.setText("✔ Thành viên uy tín 5 sao");
                    }
                }

                if (ivAvatar != null) {
                    if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                        Glide.with(ProfileActivity.this)
                                .load(user.getAvatarUrl())
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .into(ivAvatar);
                    } else if (currentUser.getPhotoUrl() != null) {
                        Glide.with(ProfileActivity.this)
                                .load(currentUser.getPhotoUrl())
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .into(ivAvatar);
                    }
                }
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
        // 1. Sách đã tặng / Bài đăng của tôi (Count books where ownerId == uid)
        FirebaseFirestore.getInstance().collection("books")
                .whereEqualTo("ownerId", uid)
                .addSnapshotListener((value, error) -> {
                    if (error == null && value != null) {
                        int count = value.size();
                        if (tvBooksGivenCount != null) {
                            tvBooksGivenCount.setText(String.valueOf(count));
                        }
                        if (tvMyPostsCount != null) {
                            tvMyPostsCount.setText(count + " đang hiển thị");
                        }
                    } else {
                        if (tvBooksGivenCount != null) tvBooksGivenCount.setText("0");
                        if (tvMyPostsCount != null) tvMyPostsCount.setText("0 đang hiển thị");
                    }
                });

        // 2. Sách đã nhận (Count requests where requesterId == uid)
        FirebaseFirestore.getInstance().collection("requests")
                .whereEqualTo("requesterId", uid)
                .addSnapshotListener((value, error) -> {
                    if (error == null && value != null) {
                        int count = value.size();
                        if (tvBooksReceivedCount != null) {
                            tvBooksReceivedCount.setText(String.valueOf(count));
                        }
                    } else {
                        if (tvBooksReceivedCount != null) tvBooksReceivedCount.setText("0");
                    }
                });

        // 3. Đánh giá (Default 0.0 (0) if none)
        if (tvRatingCount != null) {
            tvRatingCount.setText("0.0 (0)");
        }

        // 4. Sách yêu thích (Observe favorites count)
        favoriteViewModel.getFavoritedBooksOffline().observe(this, books -> {
            int count = (books != null) ? books.size() : 0;
            if (tvFavoriteCount != null) {
                tvFavoriteCount.setText(count + " cuốn");
            }
        });
    }

    private void uploadAvatarImage(Uri imageUri) {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) return;

        Toast.makeText(this, "Đang tải ảnh đại diện lên...", Toast.LENGTH_SHORT).show();

        userRepository.uploadAvatar(currentUser.getUid(), imageUri, new UserRepository.UploadCallback() {
            @Override
            public void onSuccess(String avatarUrl) {
                Toast.makeText(ProfileActivity.this, "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show();
                if (ivAvatar != null) {
                    Glide.with(ProfileActivity.this)
                            .load(avatarUrl)
                            .placeholder(R.drawable.ic_person)
                            .into(ivAvatar);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ProfileActivity.this, "Tải ảnh đại diện thất bại: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleLogout() {
        android.content.SharedPreferences prefs = getSharedPreferences("BookSharePrefs", MODE_PRIVATE);
        prefs.edit().remove("user_fullname").apply();
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
