package vn.edu.ueh.rtmiue20.project.ui.home;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.NotificationItem;
import vn.edu.ueh.rtmiue20.project.util.ViewPressEffectHelper;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationItem item);
    }

    private final List<NotificationItem> items = new ArrayList<>();
    private final OnNotificationClickListener listener;

    public NotificationAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<NotificationItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem item = items.get(position);
        Context context = holder.itemView.getContext();

        holder.tvTitle.setText(item.getTitle());
        holder.tvMessage.setText(item.getMessage());
        holder.tvTime.setText(item.getTimeAgo());

        // Status Tag
        if (item.getStatusTag() != null && !item.getStatusTag().isEmpty()) {
            holder.tvStatusTag.setText(item.getStatusTag());
            holder.tvStatusTag.setVisibility(View.VISIBLE);
            if (item.getStatusBgRes() != 0) {
                holder.tvStatusTag.setBackgroundResource(item.getStatusBgRes());
            }
            if (item.getStatusTextColorRes() != 0) {
                holder.tvStatusTag.setTextColor(ContextCompat.getColor(context, item.getStatusTextColorRes()));
            }
        } else {
            holder.tvStatusTag.setVisibility(View.GONE);
        }

        // Sub message (lời nhắn hoặc ghi chú)
        if (item.getSubMessage() != null && !item.getSubMessage().trim().isEmpty()) {
            holder.tvSubMessage.setText(item.getSubMessage().trim());
            holder.tvSubMessage.setVisibility(View.VISIBLE);
        } else {
            holder.tvSubMessage.setVisibility(View.GONE);
        }

        // Unread Dot
        holder.viewUnreadDot.setVisibility(item.isActionRequired() ? View.VISIBLE : View.GONE);

        // Action hint text
        if ("BOOK_DETAIL".equalsIgnoreCase(item.getTargetScreen())) {
            holder.tvActionHint.setText("Chạm để xem chi tiết sách");
        } else if ("sent".equalsIgnoreCase(item.getTargetTab())) {
            holder.tvActionHint.setText("Chạm để mở trang yêu cầu đã gửi");
        } else {
            holder.tvActionHint.setText("Chạm để mở trang yêu cầu nhận được");
        }

        // Image / Avatar
        holder.ivIcon.setImageTintList(null);
        if (item.getImageUrl() != null && !item.getImageUrl().trim().isEmpty()) {
            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.bg_avatar_placeholder)
                    .error(R.drawable.ic_notifications)
                    .centerCrop()
                    .into(holder.ivIcon);
        } else {
            // Default icon based on type
            if (NotificationItem.TYPE_REQUEST_ACCEPTED.equals(item.getType())) {
                holder.ivIcon.setImageResource(R.drawable.ic_verified);
                holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.secondary_green));
            } else if (NotificationItem.TYPE_REQUEST_COMPLETED.equals(item.getType())) {
                holder.ivIcon.setImageResource(R.drawable.ic_handshake);
                holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.badge_completed_text));
            } else if (NotificationItem.TYPE_REQUEST_REJECTED.equals(item.getType())) {
                holder.ivIcon.setImageResource(R.drawable.ic_close);
                holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.red_danger));
            } else {
                holder.ivIcon.setImageResource(R.drawable.ic_notifications);
                holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.primary));
            }
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(item);
            }
        });

        ViewPressEffectHelper.applyPressEffect(holder.itemView);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        View viewUnreadDot;
        TextView tvStatusTag, tvTime, tvTitle, tvMessage, tvSubMessage, tvActionHint;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivNotificationIcon);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
            tvStatusTag = itemView.findViewById(R.id.tvNotificationStatusTag);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvSubMessage = itemView.findViewById(R.id.tvNotificationSubMessage);
            tvActionHint = itemView.findViewById(R.id.tvNotificationActionHint);
        }
    }
}
