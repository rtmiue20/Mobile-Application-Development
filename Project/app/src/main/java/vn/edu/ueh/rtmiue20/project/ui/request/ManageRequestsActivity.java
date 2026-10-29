package vn.edu.ueh.rtmiue20.project.ui.request;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
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
import vn.edu.ueh.rtmiue20.project.util.ViewPressEffectHelper;

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
        } else {
            if (tvTabReceived != null) {
                tvTabReceived.setText(R.string.tab_requests_received);
            }
            if (tvTabSent != null) {
                tvTabSent.setText(R.string.tab_requests_sent);
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

            @Override
            public void onCompleteAndRate(Request request) {
                showRatingDialog(request);
            }

            @Override
            public void onOwnerComplete(Request request) {
                new AlertDialog.Builder(ManageRequestsActivity.this)
                        .setTitle("Xác nhận bàn giao sách")
                        .setMessage("Bạn đã gặp và trao cuốn \"" + (request.getBookTitle() != null ? request.getBookTitle() : "sách") + "\" cho bạn đọc " + (request.getRequesterName() != null ? request.getRequesterName() : "") + " chưa?")
                        .setPositiveButton("Đã bàn giao", (dialog, which) -> {
                            viewModel.updateRequestStatus(request.getRequestId(), Request.STATUS_COMPLETED);
                            Toast.makeText(ManageRequestsActivity.this, "Đã xác nhận hoàn thành! Đang chờ bạn đọc đánh giá.", Toast.LENGTH_LONG).show();
                        })
                        .setNegativeButton("Chưa", null)
                        .show();
            }

            @Override
            public void onConfirmReceive(Request request) {
                showConfirmReceiveDialog(request);
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

    /**
     * Mini window xác nhận đã nhận sách hay chưa dành cho người gửi yêu cầu (Tab Đã gửi).
     */
    private void showConfirmReceiveDialog(final Request request) {
        if (isFinishing() || isDestroyed() || request == null) return;

        if (Request.STATUS_COMPLETED.equalsIgnoreCase(request.getStatus())) {
            Toast.makeText(this, R.string.err_cannot_confirm_completed, Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm_receive, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView tvConfirmBookTitle = dialogView.findViewById(R.id.tvConfirmBookTitle);
        TextView tvConfirmOwnerName = dialogView.findViewById(R.id.tvConfirmOwnerName);
        TextView tvConfirmAppointment = dialogView.findViewById(R.id.tvConfirmAppointment);

        View layoutOptionReceived = dialogView.findViewById(R.id.layoutOptionReceived);
        RadioButton rbOptionReceived = dialogView.findViewById(R.id.rbOptionReceived);
        View layoutOptionNotReceived = dialogView.findViewById(R.id.layoutOptionNotReceived);
        RadioButton rbOptionNotReceived = dialogView.findViewById(R.id.rbOptionNotReceived);

        TextInputLayout tilNotReceivedNote = dialogView.findViewById(R.id.tilNotReceivedNote);
        TextInputEditText etNotReceivedNote = dialogView.findViewById(R.id.etNotReceivedNote);

        ProgressBar pbLoading = dialogView.findViewById(R.id.pbConfirmReceiveLoading);
        TextView btnCancel = dialogView.findViewById(R.id.btnCancelConfirmReceive);
        View btnSubmit = dialogView.findViewById(R.id.btnSubmitConfirmReceive);
        TextView tvSubmitText = dialogView.findViewById(R.id.tvSubmitConfirmText);

        String bTitle = request.getBookTitle() != null ? request.getBookTitle() : "sách";
        String oName = request.getOwnerName() != null ? request.getOwnerName() : "Chủ sách";
        tvConfirmBookTitle.setText(getString(R.string.label_confirm_book_title, bTitle));
        tvConfirmOwnerName.setText(getString(R.string.label_confirm_owner_name, oName));
        tvConfirmAppointment.setText(getString(R.string.label_confirm_appointment, "UEH Cơ sở B (279 Nguyễn Tri Phương, Q.10)"));

        Runnable updateOptionVisuals = () -> {
            boolean isReceived = rbOptionReceived.isChecked();
            rbOptionReceived.setChecked(isReceived);
            rbOptionNotReceived.setChecked(!isReceived);

            if (isReceived) {
                layoutOptionReceived.setBackgroundResource(R.drawable.bg_badge_active);
                layoutOptionNotReceived.setBackgroundResource(R.drawable.bg_pill_inactive);
                tilNotReceivedNote.setVisibility(View.GONE);
            } else {
                layoutOptionReceived.setBackgroundResource(R.drawable.bg_pill_inactive);
                layoutOptionNotReceived.setBackgroundResource(R.drawable.bg_reason_box);
                tilNotReceivedNote.setVisibility(View.VISIBLE);
                etNotReceivedNote.requestFocus();
            }
        };

        layoutOptionReceived.setOnClickListener(v -> {
            rbOptionReceived.setChecked(true);
            rbOptionNotReceived.setChecked(false);
            updateOptionVisuals.run();
        });
        rbOptionReceived.setOnClickListener(v -> {
            rbOptionReceived.setChecked(true);
            rbOptionNotReceived.setChecked(false);
            updateOptionVisuals.run();
        });

        layoutOptionNotReceived.setOnClickListener(v -> {
            rbOptionReceived.setChecked(false);
            rbOptionNotReceived.setChecked(true);
            updateOptionVisuals.run();
        });
        rbOptionNotReceived.setOnClickListener(v -> {
            rbOptionReceived.setChecked(false);
            rbOptionNotReceived.setChecked(true);
            updateOptionVisuals.run();
        });

        ViewPressEffectHelper.applyPressEffect(btnCancel, btnSubmit);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            btnSubmit.setEnabled(false);
            btnCancel.setEnabled(false);
            pbLoading.setVisibility(View.VISIBLE);
            tvSubmitText.setText(R.string.btn_processing);

            if (rbOptionReceived.isChecked()) {
                // Lựa chọn A: Đã nhận sách -> chuyển sang hoàn tất và mở rating
                viewModel.confirmReceivedBook(request, new RequestRepository.RequestCallback() {
                    @Override
                    public void onSuccess() {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(ManageRequestsActivity.this, R.string.msg_confirm_received_success, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        showRatingDialog(request);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (isFinishing() || isDestroyed()) return;
                        btnSubmit.setEnabled(true);
                        btnCancel.setEnabled(true);
                        pbLoading.setVisibility(View.GONE);
                        tvSubmitText.setText(R.string.btn_confirm_receive_action);
                        Toast.makeText(ManageRequestsActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                // Lựa chọn B: Chưa nhận được -> giữ nguyên accepted, lưu cờ và ghi chú
                String note = etNotReceivedNote.getText() != null ? etNotReceivedNote.getText().toString().trim() : "";
                viewModel.reportNotReceivedBook(request, note, new RequestRepository.RequestCallback() {
                    @Override
                    public void onSuccess() {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(ManageRequestsActivity.this, R.string.msg_report_not_received_success, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (isFinishing() || isDestroyed()) return;
                        btnSubmit.setEnabled(true);
                        btnCancel.setEnabled(true);
                        pbLoading.setVisibility(View.GONE);
                        tvSubmitText.setText(R.string.btn_confirm_receive_action);
                        Toast.makeText(ManageRequestsActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        dialog.show();
    }

    /**
     * TC-RATING: Hiển thị dialog đánh giá trao đổi sách khi người nhận bấm "Đã nhận sách"
     */
    private void showRatingDialog(Request request) {
        if (isFinishing() || isDestroyed()) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rating, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView tvRatingDialogSubtitle = dialogView.findViewById(R.id.tvRatingDialogSubtitle);
        TextView tvRatingFeedback = dialogView.findViewById(R.id.tvRatingFeedback);
        TextInputEditText etRatingComment = dialogView.findViewById(R.id.etRatingComment);
        TextView btnCancelRating = dialogView.findViewById(R.id.btnCancelRating);
        TextView btnSubmitRating = dialogView.findViewById(R.id.btnSubmitRating);

        ImageView[] starViews = new ImageView[]{
                dialogView.findViewById(R.id.ivStar1),
                dialogView.findViewById(R.id.ivStar2),
                dialogView.findViewById(R.id.ivStar3),
                dialogView.findViewById(R.id.ivStar4),
                dialogView.findViewById(R.id.ivStar5)
        };

        String bookTitle = request.getBookTitle() != null ? request.getBookTitle() : "cuốn sách";
        String ownerName = request.getOwnerName() != null ? request.getOwnerName() : "chủ sách";
        if (tvRatingDialogSubtitle != null) {
            tvRatingDialogSubtitle.setText("Bạn đã nhận \"" + bookTitle + "\" từ " + ownerName + ".\nHãy chia sẻ đánh giá trải nghiệm trao đổi này nhé:");
        }

        final int[] selectedStars = {5};

        Runnable updateStarDisplay = () -> {
            int score = selectedStars[0];
            int activeColor = android.graphics.Color.parseColor("#FFB300");
            int inactiveColor = android.graphics.Color.parseColor("#D1D5DB");

            for (int i = 0; i < starViews.length; i++) {
                if (starViews[i] != null) {
                    starViews[i].setColorFilter(i < score ? activeColor : inactiveColor);
                }
            }

            if (tvRatingFeedback != null) {
                switch (score) {
                    case 1:
                        tvRatingFeedback.setText("1.0 ★ • Rất không hài lòng");
                        tvRatingFeedback.setTextColor(android.graphics.Color.parseColor("#EF4444"));
                        break;
                    case 2:
                        tvRatingFeedback.setText("2.0 ★ • Chưa hài lòng");
                        tvRatingFeedback.setTextColor(android.graphics.Color.parseColor("#F97316"));
                        break;
                    case 3:
                        tvRatingFeedback.setText("3.0 ★ • Bình thường");
                        tvRatingFeedback.setTextColor(android.graphics.Color.parseColor("#EAB308"));
                        break;
                    case 4:
                        tvRatingFeedback.setText("4.0 ★ • Rất hài lòng");
                        tvRatingFeedback.setTextColor(android.graphics.Color.parseColor("#10B981"));
                        break;
                    case 5:
                    default:
                        tvRatingFeedback.setText("5.0 ★ • Tuyệt vời & Thân thiện!");
                        tvRatingFeedback.setTextColor(android.graphics.Color.parseColor("#059669"));
                        break;
                }
            }
        };

        for (int i = 0; i < starViews.length; i++) {
            final int starIndex = i + 1;
            if (starViews[i] != null) {
                starViews[i].setOnClickListener(v -> {
                    selectedStars[0] = starIndex;
                    updateStarDisplay.run();
                });
            }
        }

        updateStarDisplay.run();

        if (btnCancelRating != null) {
            btnCancelRating.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnSubmitRating != null) {
            btnSubmitRating.setOnClickListener(v -> {
                String comment = etRatingComment != null && etRatingComment.getText() != null
                        ? etRatingComment.getText().toString().trim()
                        : "";
                float score = (float) selectedStars[0];

                viewModel.completeAndRateRequest(request, score, comment);
                dialog.dismiss();

                Toast.makeText(ManageRequestsActivity.this,
                        "Cảm ơn bạn! Đã xác nhận nhận sách và đánh giá " + score + "★ thành công.",
                        Toast.LENGTH_LONG).show();
            });
        }

        dialog.show();
    }
}
