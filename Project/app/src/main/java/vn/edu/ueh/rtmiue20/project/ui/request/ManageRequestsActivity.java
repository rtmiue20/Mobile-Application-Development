package vn.edu.ueh.rtmiue20.project.ui.request;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.Request;
import vn.edu.ueh.rtmiue20.project.data.repository.RequestRepository;
import vn.edu.ueh.rtmiue20.project.ui.book.BookDetailActivity;
import vn.edu.ueh.rtmiue20.project.ui.book.CreatePostActivity;
import vn.edu.ueh.rtmiue20.project.ui.home.HomeActivity;
import vn.edu.ueh.rtmiue20.project.ui.profile.ProfileActivity;

public class ManageRequestsActivity extends AppCompatActivity {

    private ImageView btnBack, ivAvatarSmall;
    private TextView tvHeaderTitle, tvTabReceived, tvTabSent, tvEmpty, tvSummaryCount;
    private LinearLayout layoutSummaryBanner;
    private View bottomNavContainer;
    private RecyclerView rvRequests;

    private RequestViewModel viewModel;
    private RequestAdapter adapter;
    private String currentUserId;
    private boolean isAdminMode = false;

    private LiveData<List<Request>> currentLiveData;
    private Observer<List<Request>> requestsObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_requests);

        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Khởi tạo Repository với Context
        RequestRepository.getInstance(this);

        isAdminMode = getIntent().getBooleanExtra("is_admin_mode", false);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null && !isAdminMode) {
            Toast.makeText(this, "Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = user != null ? user.getUid() : "admin";

        btnBack = findViewById(R.id.btnBack);
        ivAvatarSmall = findViewById(R.id.ivAvatarSmall);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvTabReceived = findViewById(R.id.tvTabReceived);
        tvTabSent = findViewById(R.id.tvTabSent);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvSummaryCount = findViewById(R.id.tvSummaryCount);
        layoutSummaryBanner = findViewById(R.id.layoutSummaryBanner);
        bottomNavContainer = findViewById(R.id.bottomNavContainer);
        rvRequests = findViewById(R.id.rvRequests);

        // Chế độ Admin: Tùy biến giao diện phù hợp với việc kiểm duyệt toàn hệ thống
        if (isAdminMode) {
            if (tvHeaderTitle != null) {
                tvHeaderTitle.setText("Kiểm duyệt Yêu cầu Giao dịch");
            }
            if (tvTabReceived != null) {
                tvTabReceived.setText("Chờ kiểm duyệt");
            }
            if (tvTabSent != null) {
                tvTabSent.setText("Tất cả giao dịch");
            }
            if (bottomNavContainer != null) {
                bottomNavContainer.setVisibility(View.GONE);
            }
        }

        if (ivAvatarSmall != null) {
            ivAvatarSmall.setOnClickListener(v -> {
                startActivity(new Intent(ManageRequestsActivity.this, ProfileActivity.class));
            });
            loadAvatarSmall();
        }

        viewModel = new ViewModelProvider(this).get(RequestViewModel.class);

        // Khởi tạo Adapter
        adapter = new RequestAdapter(false, new RequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(Request request) {
                viewModel.updateRequestStatus(request.getRequestId(), Request.STATUS_ACCEPTED);
                Toast.makeText(ManageRequestsActivity.this,
                        isAdminMode ? "Admin đã phê duyệt giao dịch!" : "Đã đồng ý yêu cầu nhận sách!",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReject(Request request) {
                viewModel.updateRequestStatus(request.getRequestId(), Request.STATUS_REJECTED);
                Toast.makeText(ManageRequestsActivity.this,
                        isAdminMode ? "Admin đã từ chối yêu cầu giao dịch!" : "Đã từ chối yêu cầu.",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel(Request request) {
                new AlertDialog.Builder(ManageRequestsActivity.this)
                        .setTitle("Hủy yêu cầu")
                        .setMessage("Bạn có chắc chắn muốn hủy yêu cầu nhận cuốn \"" + (request.getBookTitle() != null ? request.getBookTitle() : "này") + "\" không?")
                        .setPositiveButton("Hủy yêu cầu", (dialog, which) -> {
                            viewModel.cancelRequest(request.getRequestId());
                            Toast.makeText(ManageRequestsActivity.this, "Đã hủy yêu cầu thành công!", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Đóng", null)
                        .show();
            }
        });
        adapter.setAdminMode(isAdminMode);
        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        rvRequests.setAdapter(adapter);

        // Observer chung cho danh sách
        requestsObserver = new Observer<List<Request>>() {
            @Override
            public void onChanged(List<Request> rawList) {
                List<Request> displayList = new ArrayList<>();
                if (rawList != null) {
                    if (isAdminMode && !adapter.isSentTabActive()) {
                        // Tab "Chờ kiểm duyệt" của Admin: Chỉ hiển thị yêu cầu pending
                        for (Request r : rawList) {
                            if (Request.STATUS_PENDING.equalsIgnoreCase(r.getStatus())) {
                                displayList.add(r);
                            }
                        }
                    } else {
                        displayList.addAll(rawList);
                    }
                }

                adapter.setRequests(displayList);
                if (displayList.isEmpty()) {
                    if (isAdminMode) {
                        tvEmpty.setText(!adapter.isSentTabActive()
                                ? "Không có yêu cầu nào đang chờ kiểm duyệt."
                                : "Chưa có yêu cầu giao dịch nào trên hệ thống.");
                    } else {
                        tvEmpty.setText(adapter.isSentTabActive()
                                ? "Bạn chưa gửi yêu cầu nhận sách nào."
                                : "Chưa có yêu cầu nào gửi đến bạn.");
                    }
                    tvEmpty.setVisibility(View.VISIBLE);
                    layoutSummaryBanner.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    int pendingCount = 0;
                    for (Request r : (rawList != null ? rawList : displayList)) {
                        if (Request.STATUS_PENDING.equalsIgnoreCase(r.getStatus())) {
                            pendingCount++;
                        }
                    }
                    if (pendingCount > 0 && !adapter.isSentTabActive()) {
                        layoutSummaryBanner.setVisibility(View.VISIBLE);
                        tvSummaryCount.setText(isAdminMode
                                ? "Hệ thống có " + pendingCount + " yêu cầu cần kiểm duyệt"
                                : "Bạn có " + pendingCount + " yêu cầu chờ phản hồi");
                    } else {
                        layoutSummaryBanner.setVisibility(View.GONE);
                    }
                }
            }
        };

        // Bắt sự kiện Click
        btnBack.setOnClickListener(v -> finish());

        tvTabReceived.setOnClickListener(v -> switchTab(false));

        tvTabSent.setOnClickListener(v -> switchTab(true));

        // Bắt sự kiện Success/Error từ ViewModel
        viewModel.getActionSuccess().observe(this, success -> {
            if (success != null && success) {
                viewModel.resetStatus();
            }
        });

        viewModel.getError().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(ManageRequestsActivity.this, message, Toast.LENGTH_LONG).show();
                viewModel.resetStatus();
            }
        });

        if (!isAdminMode) {
            setupBottomNavigation();
        }

        // Mặc định mở Tab đầu tiên
        switchTab(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAvatarSmall();
    }

    private void loadAvatarSmall() {
        if (ivAvatarSmall == null || currentUserId == null) return;
        android.content.SharedPreferences prefs = getSharedPreferences("BookSharePrefs", MODE_PRIVATE);
        if (prefs.contains("user_avatar_url")) {
            vn.edu.ueh.rtmiue20.project.util.ImageHelper.bindAvatar(
                    this, ivAvatarSmall, prefs.getString("user_avatar_url", ""), true
            );
        }
        vn.edu.ueh.rtmiue20.project.data.repository.UserRepository.getInstance().getUserProfile(
                currentUserId,
                new vn.edu.ueh.rtmiue20.project.data.repository.UserRepository.UserCallback() {
                    @Override
                    public void onSuccess(vn.edu.ueh.rtmiue20.project.data.model.User userProfile) {
                        if (isFinishing() || isDestroyed()) return;
                        String avatarUrl = userProfile != null && userProfile.getAvatarUrl() != null
                                ? userProfile.getAvatarUrl()
                                : "";
                        prefs.edit().putString("user_avatar_url", avatarUrl).apply();
                        vn.edu.ueh.rtmiue20.project.util.ImageHelper.bindAvatar(ManageRequestsActivity.this, ivAvatarSmall, avatarUrl, true);
                    }

                    @Override
                    public void onError(String errorMessage) {}
                }
        );
    }

    private void setupBottomNavigation() {
        View btnNavHome = findViewById(R.id.btnNavHome);
        View btnNavSearch = findViewById(R.id.btnNavSearch);
        View btnNavProfile = findViewById(R.id.btnNavProfile);
        View fabAddBook = findViewById(R.id.fabAddBook);

        if (btnNavHome != null) {
            btnNavHome.setOnClickListener(v -> finish());
        }
        if (btnNavSearch != null) {
            btnNavSearch.setOnClickListener(v -> {
                Intent intent = new Intent(ManageRequestsActivity.this, HomeActivity.class);
                intent.putExtra("focus_search", true);
                startActivity(intent);
                finish();
            });
        }
        if (btnNavProfile != null) {
            btnNavProfile.setOnClickListener(v -> {
                startActivity(new Intent(ManageRequestsActivity.this, ProfileActivity.class));
                finish();
            });
        }
        if (fabAddBook != null) {
            fabAddBook.setOnClickListener(v -> {
                startActivity(new Intent(ManageRequestsActivity.this, CreatePostActivity.class));
            });
        }
    }

    private void switchTab(boolean showSent) {
        if (currentLiveData != null) {
            currentLiveData.removeObserver(requestsObserver);
        }

        adapter.setSentTab(showSent);
        tvEmpty.setVisibility(View.VISIBLE);
        layoutSummaryBanner.setVisibility(View.GONE);
        updateTabStyles(showSent);

        if (isAdminMode) {
            // Chế độ Admin: Lấy toàn bộ yêu cầu của hệ thống
            currentLiveData = viewModel.getAllRequests();
        } else {
            if (showSent) {
                currentLiveData = viewModel.getSentRequests(currentUserId);
            } else {
                currentLiveData = viewModel.getReceivedRequests(currentUserId);
            }
        }
        currentLiveData.observe(this, requestsObserver);
    }

    private void updateTabStyles(boolean showSent) {
        if (showSent) {
            tvTabReceived.setBackgroundResource(android.R.color.transparent);
            tvTabReceived.setTextColor(getResources().getColor(R.color.text_secondary));
            tvTabSent.setBackgroundResource(R.drawable.bg_pill_active);
            tvTabSent.setTextColor(getResources().getColor(R.color.white));
        } else {
            tvTabReceived.setBackgroundResource(R.drawable.bg_pill_active);
            tvTabReceived.setTextColor(getResources().getColor(R.color.white));
            tvTabSent.setBackgroundResource(android.R.color.transparent);
            tvTabSent.setTextColor(getResources().getColor(R.color.text_secondary));
        }
    }
}
