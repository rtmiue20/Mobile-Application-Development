package vn.edu.ueh.rtmiue20.project.ui.admin;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.User;
import vn.edu.ueh.rtmiue20.project.data.repository.UserRepository;
import vn.edu.ueh.rtmiue20.project.util.ViewPressEffectHelper;

public class UserManagementActivity extends AppCompatActivity implements UserManageAdapter.OnUserActionListener {

    private final List<User> allUsersList = new ArrayList<>();
    private final List<User> currentFilteredList = new ArrayList<>();

    private UserManageAdapter userAdapter;
    private ListenerRegistration usersListener;

    private ScrollView scrollViewUserManagement;
    private ImageView btnBack, btnRefreshUsers, btnHeaderSort, btnClearUserSearch, ivAdminAvatar;
    private TextView tvTotalUsersCount, tvActiveUsersCount, tvLockedUsersCount;
    private EditText etSearchUser;

    // Filter Chips
    private TextView chipFilterAll, chipFilterMembers, chipFilterAdmins;
    private LinearLayout chipFilterLocked, btnSortUsers;
    private TextView tvChipLockedText, tvCurrentSortText;

    private RecyclerView rvUsers;
    private TextView tvEmptyUsers;
    private ProgressBar progressBarUsers;

    // Pagination
    private static final int ITEMS_PER_PAGE = 10;
    private int currentPage = 1;
    private int totalPages = 1;
    private TextView tvUsersShowingCount, btnPrevPage, btnNextPage, tvPageIndicator;

    // State
    private String currentFilter = "all"; // all, members, admins, locked
    private String currentSort = "newest"; // newest, oldest, az
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_management);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainUserManagement), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupRecyclerView();
        setupListeners();
        setupPressEffects();
        startRealtimeUsersListener();
    }

    private void initViews() {
        scrollViewUserManagement = findViewById(R.id.scrollViewUserManagement);
        btnBack = findViewById(R.id.btnBack);
        btnHeaderSort = findViewById(R.id.btnHeaderSort);
        btnRefreshUsers = findViewById(R.id.btnRefreshUsers);
        ivAdminAvatar = findViewById(R.id.ivAdminAvatar);

        tvTotalUsersCount = findViewById(R.id.tvTotalUsersCount);
        tvActiveUsersCount = findViewById(R.id.tvActiveUsersCount);
        tvLockedUsersCount = findViewById(R.id.tvLockedUsersCount);

        etSearchUser = findViewById(R.id.etSearchUser);
        btnClearUserSearch = findViewById(R.id.btnClearUserSearch);

        chipFilterAll = findViewById(R.id.chipFilterAll);
        chipFilterMembers = findViewById(R.id.chipFilterMembers);
        chipFilterAdmins = findViewById(R.id.chipFilterAdmins);
        chipFilterLocked = findViewById(R.id.chipFilterLocked);
        tvChipLockedText = findViewById(R.id.tvChipLockedText);

        btnSortUsers = findViewById(R.id.btnSortUsers);
        tvCurrentSortText = findViewById(R.id.tvCurrentSortText);

        rvUsers = findViewById(R.id.rvUsers);
        tvEmptyUsers = findViewById(R.id.tvEmptyUsers);
        progressBarUsers = findViewById(R.id.progressBarUsers);

        tvUsersShowingCount = findViewById(R.id.tvUsersShowingCount);
        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        tvPageIndicator = findViewById(R.id.tvPageIndicator);
    }

    private void setupRecyclerView() {
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserManageAdapter(this);
        rvUsers.setAdapter(userAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        if (ivAdminAvatar != null) {
            ivAdminAvatar.setOnClickListener(v -> startActivity(new Intent(UserManagementActivity.this, AdminProfileActivity.class)));
        }

        btnRefreshUsers.setOnClickListener(v -> {
            RotateAnimation rotate = new RotateAnimation(0, 360,
                    Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(500);
            btnRefreshUsers.startAnimation(rotate);
            Toast.makeText(this, "Đang đồng bộ realtime từ Firestore users collection...", Toast.LENGTH_SHORT).show();
        });

        // Search text watcher
        etSearchUser.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase();
                btnClearUserSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                currentPage = 1;
                applyFiltersAndSort();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnClearUserSearch.setOnClickListener(v -> {
            etSearchUser.setText("");
            etSearchUser.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(etSearchUser.getWindowToken(), 0);
        });

        // Filter chips click
        chipFilterAll.setOnClickListener(v -> selectFilter("all"));
        chipFilterMembers.setOnClickListener(v -> selectFilter("members"));
        chipFilterAdmins.setOnClickListener(v -> selectFilter("admins"));
        chipFilterLocked.setOnClickListener(v -> selectFilter("locked"));

        // Sort options
        btnSortUsers.setOnClickListener(v -> showSortDialog());
        btnHeaderSort.setOnClickListener(v -> showSortDialog());

        // Pagination buttons
        btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                updatePagination();
                if (scrollViewUserManagement != null) scrollViewUserManagement.smoothScrollTo(0, rvUsers.getTop());
            }
        });

        btnNextPage.setOnClickListener(v -> {
            if (currentPage < totalPages) {
                currentPage++;
                updatePagination();
                if (scrollViewUserManagement != null) scrollViewUserManagement.smoothScrollTo(0, rvUsers.getTop());
            }
        });
    }

    private void setupPressEffects() {
        ViewPressEffectHelper.applyPressEffect(
                btnBack, btnRefreshUsers, btnHeaderSort, btnClearUserSearch, ivAdminAvatar,
                chipFilterAll, chipFilterMembers, chipFilterAdmins, chipFilterLocked,
                btnSortUsers, btnPrevPage, btnNextPage
        );
    }

    private void startRealtimeUsersListener() {
        progressBarUsers.setVisibility(View.VISIBLE);
        usersListener = UserRepository.getInstance().getAllUsersRealtime(new UserRepository.UsersListCallback() {
            @Override
            public void onSuccess(List<User> userList) {
                progressBarUsers.setVisibility(View.GONE);
                allUsersList.clear();
                if (userList != null) {
                    allUsersList.addAll(userList);
                }
                updateCounters();
                applyFiltersAndSort();
            }

            @Override
            public void onError(String errorMessage) {
                progressBarUsers.setVisibility(View.GONE);
                Toast.makeText(UserManagementActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateCounters() {
        int total = allUsersList.size();
        int active = 0;
        int locked = 0;
        int members = 0;
        int admins = 0;

        for (User u : allUsersList) {
            if ("locked".equalsIgnoreCase(u.getStatus())) {
                locked++;
            } else {
                active++;
            }

            if ("admin".equalsIgnoreCase(u.getRole())) {
                admins++;
            } else {
                members++;
            }
        }

        tvTotalUsersCount.setText(String.valueOf(total));
        tvActiveUsersCount.setText(String.valueOf(active));
        tvLockedUsersCount.setText(String.valueOf(locked));

        // Update chip texts with counts
        chipFilterAll.setText("all".equals(currentFilter) ? "✓ Tất cả (" + total + ")" : "Tất cả (" + total + ")");
        chipFilterMembers.setText("members".equals(currentFilter) ? "✓ Thành viên (" + members + ")" : "Thành viên (" + members + ")");
        chipFilterAdmins.setText("admins".equals(currentFilter) ? "✓ Admin (" + admins + ")" : "Admin (" + admins + ")");
        tvChipLockedText.setText("locked".equals(currentFilter) ? "✓ Bị khóa (" + locked + ")" : "Bị khóa (" + locked + ")");
    }

    private void selectFilter(String filter) {
        currentFilter = filter;
        resetChipStyles();

        switch (filter) {
            case "all":
                chipFilterAll.setBackgroundResource(R.drawable.bg_pill_active);
                chipFilterAll.setTextColor(getResources().getColor(R.color.on_primary));
                chipFilterAll.setTypeface(null, Typeface.BOLD);
                break;
            case "members":
                chipFilterMembers.setBackgroundResource(R.drawable.bg_pill_active);
                chipFilterMembers.setTextColor(getResources().getColor(R.color.on_primary));
                chipFilterMembers.setTypeface(null, Typeface.BOLD);
                break;
            case "admins":
                chipFilterAdmins.setBackgroundResource(R.drawable.bg_pill_active);
                chipFilterAdmins.setTextColor(getResources().getColor(R.color.on_primary));
                chipFilterAdmins.setTypeface(null, Typeface.BOLD);
                break;
            case "locked":
                chipFilterLocked.setBackgroundResource(R.drawable.bg_pill_active);
                tvChipLockedText.setTextColor(getResources().getColor(R.color.on_primary));
                tvChipLockedText.setTypeface(null, Typeface.BOLD);
                break;
        }

        updateCounters();
        currentPage = 1;
        applyFiltersAndSort();
    }

    private void resetChipStyles() {
        chipFilterAll.setBackgroundResource(R.drawable.bg_pill_inactive);
        chipFilterAll.setTextColor(getResources().getColor(R.color.on_surface_variant));
        chipFilterAll.setTypeface(null, Typeface.NORMAL);

        chipFilterMembers.setBackgroundResource(R.drawable.bg_pill_inactive);
        chipFilterMembers.setTextColor(getResources().getColor(R.color.on_surface_variant));
        chipFilterMembers.setTypeface(null, Typeface.NORMAL);

        chipFilterAdmins.setBackgroundResource(R.drawable.bg_pill_inactive);
        chipFilterAdmins.setTextColor(getResources().getColor(R.color.on_surface_variant));
        chipFilterAdmins.setTypeface(null, Typeface.NORMAL);

        chipFilterLocked.setBackgroundResource(R.drawable.bg_pill_inactive);
        tvChipLockedText.setTextColor(getResources().getColor(R.color.on_surface_variant));
        tvChipLockedText.setTypeface(null, Typeface.NORMAL);
    }

    private void showSortDialog() {
        String[] options = {"Mới nhất", "Cũ nhất", "Tên người dùng (A-Z)"};
        int checkedItem = 0;
        if ("oldest".equals(currentSort)) checkedItem = 1;
        else if ("az".equals(currentSort)) checkedItem = 2;

        new AlertDialog.Builder(this)
                .setTitle("Sắp xếp danh sách")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    if (which == 0) {
                        currentSort = "newest";
                        tvCurrentSortText.setText("Mới nhất");
                    } else if (which == 1) {
                        currentSort = "oldest";
                        tvCurrentSortText.setText("Cũ nhất");
                    } else {
                        currentSort = "az";
                        tvCurrentSortText.setText("Tên (A-Z)");
                    }
                    dialog.dismiss();
                    applyFiltersAndSort();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void applyFiltersAndSort() {
        currentFilteredList.clear();

        for (User u : allUsersList) {
            // 1. Role / Status Filter
            if ("members".equals(currentFilter) && "admin".equalsIgnoreCase(u.getRole())) {
                continue;
            }
            if ("admins".equals(currentFilter) && !"admin".equalsIgnoreCase(u.getRole())) {
                continue;
            }
            if ("locked".equals(currentFilter) && !"locked".equalsIgnoreCase(u.getStatus())) {
                continue;
            }

            // 2. Search query filter
            if (!searchQuery.isEmpty()) {
                String name = u.getFullName() != null ? u.getFullName().toLowerCase() : "";
                String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
                String uid = u.getUid() != null ? u.getUid().toLowerCase() : "";
                if (!name.contains(searchQuery) && !email.contains(searchQuery) && !uid.contains(searchQuery)) {
                    continue;
                }
            }

            currentFilteredList.add(u);
        }

        // 3. Sorting
        if ("oldest".equals(currentSort)) {
            currentFilteredList.sort((u1, u2) -> Long.compare(u1.getCreatedAt(), u2.getCreatedAt()));
        } else if ("az".equals(currentSort)) {
            currentFilteredList.sort((u1, u2) -> {
                String n1 = u1.getFullName() != null ? u1.getFullName() : "";
                String n2 = u2.getFullName() != null ? u2.getFullName() : "";
                return n1.compareToIgnoreCase(n2);
            });
        } else {
            // Default: newest
            currentFilteredList.sort((u1, u2) -> Long.compare(u2.getCreatedAt(), u1.getCreatedAt()));
        }

        // 4. Update pagination
        totalPages = (int) Math.ceil((double) currentFilteredList.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        if (currentPage > totalPages) currentPage = totalPages;

        updatePagination();
    }

    private void updatePagination() {
        if (currentFilteredList.isEmpty()) {
            tvEmptyUsers.setVisibility(View.VISIBLE);
            rvUsers.setVisibility(View.GONE);
            findViewById(R.id.layoutPagination).setVisibility(View.GONE);
            return;
        }

        tvEmptyUsers.setVisibility(View.GONE);
        rvUsers.setVisibility(View.VISIBLE);
        findViewById(R.id.layoutPagination).setVisibility(View.VISIBLE);

        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, currentFilteredList.size());

        List<User> pageItems = currentFilteredList.subList(startIndex, endIndex);
        userAdapter.updateUserList(pageItems);

        tvUsersShowingCount.setText("Hiển thị " + (startIndex + 1) + " - " + endIndex + " trong tổng số " + allUsersList.size() + " người dùng");
        tvPageIndicator.setText(currentPage + " / " + totalPages);

        btnPrevPage.setVisibility(currentPage > 1 ? View.VISIBLE : View.INVISIBLE);
        btnNextPage.setVisibility(currentPage < totalPages ? View.VISIBLE : View.INVISIBLE);
    }

    // --- OnUserActionListener Implementation ---

    @Override
    public void onLock(User user) {
        showLockUserModal(user);
    }

    @Override
    public void onUnlock(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Mở khóa người dùng")
                .setMessage("Bạn có chắc muốn phục hồi quyền truy cập cho " + (user.getFullName() != null ? user.getFullName() : user.getEmail()) + "? Người dùng sẽ có thể đăng nhập bình thường.")
                .setPositiveButton("Mở khóa", (dialog, which) -> {
                    UserRepository.getInstance().updateUserStatus(user.getUid(), "active", null, new UserRepository.UpdateCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(UserManagementActivity.this, "Đã mở khóa tài khoản thành công!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(UserManagementActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showLockUserModal(User user) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_lock_user, null);
        dialog.setContentView(sheetView);

        TextView tvSubtitle = sheetView.findViewById(R.id.tvModalSubtitle);
        TextView tvDescription = sheetView.findViewById(R.id.tvModalDescription);
        Spinner spLockReason = sheetView.findViewById(R.id.spLockReason);
        EditText etCustomReason = sheetView.findViewById(R.id.etCustomReason);
        TextView btnCancel = sheetView.findViewById(R.id.btnCancelLock);
        View btnConfirm = sheetView.findViewById(R.id.btnConfirmLock);

        String name = user.getFullName() != null ? user.getFullName() : "Người dùng";
        String uid = user.getUid() != null ? user.getUid() : "";
        tvSubtitle.setText("UID: " + uid + " • Firestore users collection");
        tvDescription.setText("Bạn có chắc muốn đình chỉ tài khoản " + name + "? Người dùng sẽ bị hệ thống tự động từ chối đăng nhập và phiên làm việc hiện tại bị hủy lập tức qua AuthRepository.");

        String[] reasons = {
                "Spam bài đăng hoặc gửi tin nhắn rác",
                "Thông tin tài khoản giả mạo",
                "Hành vi gian lận sách / không hoàn trả sách mượn",
                "Vi phạm tiêu chuẩn cộng đồng BookShare",
                "Lý do khác..."
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, reasons);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLockReason.setAdapter(adapter);

        spLockReason.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                etCustomReason.setVisibility(position == reasons.length - 1 ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String selectedReason = (String) spLockReason.getSelectedItem();
            if (etCustomReason.getVisibility() == View.VISIBLE) {
                String custom = etCustomReason.getText().toString().trim();
                if (!custom.isEmpty()) {
                    selectedReason = custom;
                }
            }

            final String finalReason = selectedReason;
            UserRepository.getInstance().updateUserStatus(user.getUid(), "locked", finalReason, new UserRepository.UpdateCallback() {
                @Override
                public void onSuccess() {
                    dialog.dismiss();
                    Toast.makeText(UserManagementActivity.this, "Đã khóa tài khoản " + name + " thành công!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(UserManagementActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });

        ViewPressEffectHelper.applyPressEffect(btnCancel, btnConfirm);
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (usersListener != null) {
            usersListener.remove();
            usersListener = null;
        }
    }
}
