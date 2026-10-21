package vn.edu.ueh.rtmiue20.project.ui.request;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.Request;

public class ManageRequestsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvTabReceived, tvTabSent, tvEmpty, tvSummaryCount;
    private LinearLayout layoutSummaryBanner;
    private RecyclerView rvRequests;

    private RequestViewModel viewModel;
    private RequestAdapter adapter;
    private String currentUserId;

    private LiveData<List<Request>> currentLiveData;
    private Observer<List<Request>> requestsObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_requests);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = user.getUid();

        btnBack = findViewById(R.id.btnBack);
        tvTabReceived = findViewById(R.id.tvTabReceived);
        tvTabSent = findViewById(R.id.tvTabSent);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvSummaryCount = findViewById(R.id.tvSummaryCount);
        layoutSummaryBanner = findViewById(R.id.layoutSummaryBanner);
        rvRequests = findViewById(R.id.rvRequests);

        viewModel = new ViewModelProvider(this).get(RequestViewModel.class);

        // Khởi tạo Adapter
        adapter = new RequestAdapter(false, new RequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(Request request) {
                viewModel.updateRequestStatus(request.getRequestId(), Request.STATUS_ACCEPTED);
            }

            @Override
            public void onReject(Request request) {
                viewModel.updateRequestStatus(request.getRequestId(), Request.STATUS_REJECTED);
            }
        });
        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        rvRequests.setAdapter(adapter);

        // Observer chung cho danh sách
        requestsObserver = new Observer<List<Request>>() {
            @Override
            public void onChanged(List<Request> requests) {
                adapter.setRequests(requests);
                if (requests == null || requests.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    layoutSummaryBanner.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    // Chỉ đếm các request "pending" để hiện lên Banner
                    int pendingCount = 0;
                    for (Request r : requests) {
                        if (Request.STATUS_PENDING.equals(r.getStatus())) {
                            pendingCount++;
                        }
                    }
                    if (pendingCount > 0 && !adapter.isSentTabActive()) {
                        layoutSummaryBanner.setVisibility(View.VISIBLE);
                        tvSummaryCount.setText("Bạn có " + pendingCount + " yêu cầu chờ phản hồi");
                    } else {
                        layoutSummaryBanner.setVisibility(View.GONE);
                    }
                }
            }
        };

        // Bắt sự kiện Click (No Lambda)
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        tvTabReceived.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(false);
            }
        });

        tvTabSent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(true);
            }
        });

        // Bắt sự kiện Success/Error từ ViewModel
        viewModel.getActionSuccess().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                if (success != null && success) {
                    Toast.makeText(ManageRequestsActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    viewModel.resetStatus();
                }
            }
        });

        viewModel.getError().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String message) {
                if (message != null) {
                    Toast.makeText(ManageRequestsActivity.this, message, Toast.LENGTH_LONG).show();
                    viewModel.resetStatus();
                }
            }
        });

        // Mặc định mở Tab Yêu cầu đến
        switchTab(false);
    }

    private void switchTab(boolean showSent) {
        if (currentLiveData != null) {
            currentLiveData.removeObserver(requestsObserver);
        }

        adapter.setSentTab(showSent);
        tvEmpty.setVisibility(View.GONE);
        layoutSummaryBanner.setVisibility(View.GONE);
        updateTabStyles(showSent);

        if (showSent) {
            currentLiveData = viewModel.getSentRequests(currentUserId);
        } else {
            currentLiveData = viewModel.getReceivedRequests(currentUserId);
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