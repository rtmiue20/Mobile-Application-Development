package vn.edu.ueh.rtmiue20.project.ui.request;

import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.Request;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    public interface OnRequestActionListener {
        void onAccept(Request request);
        void onReject(Request request);
    }

    private final List<Request> requests = new ArrayList<>();
    private final OnRequestActionListener listener;
    private boolean isSentTab;

    public RequestAdapter(boolean isSentTab, OnRequestActionListener listener) {
        this.isSentTab = isSentTab;
        this.listener = listener;
    }

    public void setSentTab(boolean isSentTab) {
        this.isSentTab = isSentTab;
        this.requests.clear();
        notifyDataSetChanged();
    }

    public boolean isSentTabActive() {
        return isSentTab;
    }

    public void setRequests(List<Request> newRequests) {
        requests.clear();
        if (newRequests != null) {
            requests.addAll(newRequests);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        final Request request = requests.get(position);

        holder.tvBookTitle.setText(request.getBookTitle() != null ? request.getBookTitle() : "Sách không tên");
        holder.tvMessage.setText(request.getMessage() != null ? request.getMessage() : "Xin chào, mình rất muốn nhận cuốn sách này!");

        // Load Book Cover
        if (request.getBookImageUrl() != null && !request.getBookImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext()).load(request.getBookImageUrl()).into(holder.ivBookCover);
        } else {
            holder.ivBookCover.setImageResource(R.drawable.ic_bookshare_logo);
        }

        // Format Timestamp
        holder.tvTimestamp.setText(DateUtils.getRelativeTimeSpanString(
                request.getTimestamp(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));

        // TẨY TRẮNG GIAO DIỆN TRƯỚC KHI HIỂN THỊ LẠI
        holder.btnReject.setVisibility(View.GONE);
        holder.btnAccept.setVisibility(View.GONE);
        holder.btnCancelRequest.setVisibility(View.GONE);
        holder.btnRemind.setVisibility(View.GONE);
        holder.btnViewQR.setVisibility(View.GONE);
        holder.layoutAppointment.setVisibility(View.GONE);
        holder.tvBookTag.setVisibility(View.GONE);

        // UI Logic dựa vào Tab (isSentTab)
        if (isSentTab) {
            // TAB "ĐÃ GỬI" (Người xin sách xem)
            holder.tvPersonName.setText("Gửi tới: " + (request.getOwnerName() != null ? request.getOwnerName() : "Chủ sách"));
            holder.ivUserAvatar.setImageResource(R.drawable.ic_person); // Giả lập avatar chủ sách

            if (Request.STATUS_PENDING.equals(request.getStatus())) {
                setupStatusBadge(holder.tvStatusBadge, "Chờ phản hồi", "#E65100", "#FFF3E0");
                holder.btnCancelRequest.setVisibility(View.VISIBLE);
                holder.btnRemind.setVisibility(View.VISIBLE);
            } else if (Request.STATUS_ACCEPTED.equals(request.getStatus())) {
                setupStatusBadge(holder.tvStatusBadge, "✓ Đã chấp nhận", "#1B5E20", "#E8F5E9");
                holder.layoutAppointment.setVisibility(View.VISIBLE); // Hiện khối Điểm hẹn
                holder.btnViewQR.setVisibility(View.VISIBLE); // Hiện nút QR màu xanh
            } else {
                setupStatusBadge(holder.tvStatusBadge, "✕ Đã từ chối", "#B71C1C", "#FFEBEE");
                holder.tvMessage.setText("Lý do: Sách đã được trao cho độc giả khác trước đó.");
            }

        } else {
            // TAB "YÊU CẦU ĐẾN" (Chủ sách xem)
            holder.tvPersonName.setText(request.getRequesterName() != null ? request.getRequesterName() : "Bạn đọc");
            if (request.getRequesterAvatar() != null && !request.getRequesterAvatar().isEmpty()) {
                Glide.with(holder.itemView.getContext()).load(request.getRequesterAvatar()).into(holder.ivUserAvatar);
            } else {
                holder.ivUserAvatar.setImageResource(R.drawable.ic_person);
            }

            // Hiện nhãn phụ của sách
            holder.tvBookTag.setVisibility(View.VISIBLE);

            if (Request.STATUS_PENDING.equals(request.getStatus())) {
                setupStatusBadge(holder.tvStatusBadge, "Chờ phản hồi", "#E65100", "#FFF3E0");
                holder.btnReject.setVisibility(View.VISIBLE);
                holder.btnAccept.setVisibility(View.VISIBLE);
            } else if (Request.STATUS_ACCEPTED.equals(request.getStatus())) {
                setupStatusBadge(holder.tvStatusBadge, "Đã đồng ý", "#1B5E20", "#E8F5E9");
            } else {
                setupStatusBadge(holder.tvStatusBadge, "Đã từ chối", "#B71C1C", "#FFEBEE");
            }
        }

        // Bắt sự kiện Click (No Lambda)
        holder.btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onAccept(request);
            }
        });

        holder.btnReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onReject(request);
            }
        });
    }

    private void setupStatusBadge(TextView badge, String text, String textColorHex, String bgColorHex) {
        badge.setText(text);
        badge.setTextColor(Color.parseColor(textColorHex));
        badge.setBackgroundColor(Color.parseColor(bgColorHex));
    }

    @Override
    public int getItemCount() { return requests.size(); }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvTimestamp, tvBookTag, tvStatusBadge, tvBookTitle, tvAuthorMock;
        TextView tvPersonName, tvDistanceMock, tvMessage;
        ImageView ivTimeIcon, ivBookCover, ivUserAvatar;
        LinearLayout layoutMessage, layoutAppointment, layoutActions;
        TextView btnReject, btnAccept, btnCancelRequest, btnRemind, btnViewQR;

        RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvBookTag = itemView.findViewById(R.id.tvBookTag);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvBookTitle = itemView.findViewById(R.id.tvBookTitle);
            tvAuthorMock = itemView.findViewById(R.id.tvAuthorMock);
            tvPersonName = itemView.findViewById(R.id.tvPersonName);
            tvDistanceMock = itemView.findViewById(R.id.tvDistanceMock);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            ivTimeIcon = itemView.findViewById(R.id.ivTimeIcon);
            ivBookCover = itemView.findViewById(R.id.ivBookCover);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            layoutMessage = itemView.findViewById(R.id.layoutMessage);
            layoutAppointment = itemView.findViewById(R.id.layoutAppointment);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnCancelRequest = itemView.findViewById(R.id.btnCancelRequest);
            btnRemind = itemView.findViewById(R.id.btnRemind);
            btnViewQR = itemView.findViewById(R.id.btnViewQR);
        }
    }
}