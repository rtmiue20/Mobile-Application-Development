package vn.edu.ueh.rtmiue20.project.ui.admin;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.User;
import vn.edu.ueh.rtmiue20.project.util.ViewPressEffectHelper;

public class UserManageAdapter extends RecyclerView.Adapter<UserManageAdapter.UserViewHolder> {

    public interface OnUserActionListener {
        void onLock(User user);
        void onUnlock(User user);
    }

    private List<User> userList = new ArrayList<>();
    private final OnUserActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public UserManageAdapter(OnUserActionListener listener) {
        this.listener = listener;
    }

    public void updateUserList(List<User> list) {
        this.userList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_manage, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        Context context = holder.itemView.getContext();

        // 1. Full name
        String name = user.getFullName() != null && !user.getFullName().trim().isEmpty()
                ? user.getFullName()
                : "Người dùng BookShare";
        holder.tvFullName.setText(name);

        // 2. Email & UID
        holder.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "Chưa có email");
        String uid = user.getUid() != null ? user.getUid() : "";
        String shortUid = uid.length() > 8 ? uid.substring(0, 8) + "..." : uid;
        holder.tvUid.setText(shortUid);

        // 3. Created At Date
        if (user.getCreatedAt() > 0) {
            holder.tvCreatedAt.setText("Tham gia: " + dateFormat.format(new Date(user.getCreatedAt())));
        } else {
            holder.tvCreatedAt.setText("Tham gia: Mới đây");
        }

        // 4. Role
        boolean isAdmin = "admin".equalsIgnoreCase(user.getRole());
        if (isAdmin) {
            holder.tvAdminBadge.setVisibility(View.VISIBLE);
            holder.tvRoleTag.setText("Quản trị viên");
        } else {
            holder.tvAdminBadge.setVisibility(View.GONE);
            holder.tvRoleTag.setText("Thành viên");
        }

        // 5. Status & Lock/Unlock state
        boolean isLocked = "locked".equalsIgnoreCase(user.getStatus());
        if (isLocked) {
            holder.tvFullName.setPaintFlags(holder.tvFullName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvFullName.setAlpha(0.7f);

            holder.tvStatusBadge.setText("⚠ locked");
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_locked);
            holder.tvStatusBadge.setTextColor(context.getResources().getColor(R.color.error));

            holder.dotStatus.setBackgroundResource(R.drawable.bg_badge_locked);

            holder.layoutLockReason.setVisibility(View.VISIBLE);
            String reason = user.getLockReason() != null && !user.getLockReason().trim().isEmpty()
                    ? user.getLockReason()
                    : "Đình chỉ do vi phạm quy định cộng đồng BookShare.";
            holder.tvLockReason.setText("Lý do khóa: " + reason);

            holder.tvActionSubtitle.setText("Bị khóa bởi Admin");

            holder.btnLockUser.setVisibility(View.GONE);
            holder.btnUnlockUser.setVisibility(View.VISIBLE);
        } else {
            holder.tvFullName.setPaintFlags(holder.tvFullName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvFullName.setAlpha(1.0f);

            holder.tvStatusBadge.setText("✓ active");
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_active);
            holder.tvStatusBadge.setTextColor(context.getResources().getColor(R.color.secondary));

            holder.dotStatus.setBackgroundResource(R.drawable.bg_fab_add);
            holder.layoutLockReason.setVisibility(View.GONE);

            holder.tvActionSubtitle.setText("Đang hoạt động bình thường");

            holder.btnLockUser.setVisibility(View.VISIBLE);
            holder.btnUnlockUser.setVisibility(View.GONE);
        }

        // 6. Avatar
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().trim().isEmpty()) {
            Glide.with(context)
                    .load(user.getAvatarUrl())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(holder.ivUserAvatar);
        } else {
            holder.ivUserAvatar.setImageResource(R.drawable.ic_person);
            holder.ivUserAvatar.setColorFilter(context.getResources().getColor(R.color.outline));
        }

        // 7. Actions Click Listeners
        holder.btnLockUser.setOnClickListener(v -> {
            if (listener != null) listener.onLock(user);
        });

        holder.btnUnlockUser.setOnClickListener(v -> {
            if (listener != null) listener.onUnlock(user);
        });

        ViewPressEffectHelper.applyPressEffect(holder.btnLockUser, holder.btnUnlockUser);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserAvatar;
        View dotStatus;
        TextView tvFullName, tvAdminBadge, tvStatusBadge;
        TextView tvEmail, tvUid, tvRoleTag, tvCreatedAt;
        LinearLayout layoutLockReason;
        TextView tvLockReason, tvActionSubtitle;
        LinearLayout btnLockUser, btnUnlockUser;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            dotStatus = itemView.findViewById(R.id.dotStatus);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            tvAdminBadge = itemView.findViewById(R.id.tvAdminBadge);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvUid = itemView.findViewById(R.id.tvUid);
            tvRoleTag = itemView.findViewById(R.id.tvRoleTag);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
            layoutLockReason = itemView.findViewById(R.id.layoutLockReason);
            tvLockReason = itemView.findViewById(R.id.tvLockReason);
            tvActionSubtitle = itemView.findViewById(R.id.tvActionSubtitle);
            btnLockUser = itemView.findViewById(R.id.btnLockUser);
            btnUnlockUser = itemView.findViewById(R.id.btnUnlockUser);
        }
    }
}
