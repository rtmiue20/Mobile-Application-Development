package vn.edu.ueh.rtmiue20.project.ui.book;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.Comment;
import vn.edu.ueh.rtmiue20.project.util.ImageHelper;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    public interface OnCommentActionListener {
        void onEdit(Comment comment);
        void onDelete(Comment comment);
    }

    private List<Comment> comments = new ArrayList<>();
    private final OnCommentActionListener actionListener;
    private String bookOwnerId;
    private boolean isAdmin = false;

    public CommentAdapter(OnCommentActionListener listener) {
        this.actionListener = listener;
    }

    public void setComments(List<Comment> comments) {
        this.comments = (comments == null) ? new ArrayList<>() : comments;
        notifyDataSetChanged();
    }

    public void setBookOwnerId(String bookOwnerId) {
        this.bookOwnerId = bookOwnerId;
        notifyDataSetChanged();
    }

    public void setIsAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        final Comment comment = comments.get(position);
        Context context = holder.itemView.getContext();

        holder.tvUserName.setText(comment.getUserName() != null ? comment.getUserName() : "Người dùng");
        holder.tvContent.setText(comment.getContent() != null ? comment.getContent() : "");

        // Bind Avatar người dùng
        ImageHelper.bindAvatar(context, holder.imgAvatar, comment.getUserAvatar(), true);

        // Hiển thị huy hiệu nếu người bình luận là Chủ sách
        if (bookOwnerId != null && bookOwnerId.equals(comment.getUserId())) {
            holder.tvOwnerBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvOwnerBadge.setVisibility(View.GONE);
        }

        // Thời gian đăng / sửa
        Long createdAt = comment.getCreatedAt();
        if (createdAt != null) {
            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    createdAt,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS);
            if (comment.getUpdatedAt() != null) {
                holder.tvTime.setText(relativeTime + " (đã sửa)");
            } else {
                holder.tvTime.setText(relativeTime);
            }
        } else {
            holder.tvTime.setText("");
        }

        // Quyền quản lý bình luận: Tác giả, Chủ sách, hoặc Admin
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        final boolean isCommentAuthor = currentUser != null && currentUser.getUid().equals(comment.getUserId());
        final boolean isBookOwner = currentUser != null && currentUser.getUid().equals(bookOwnerId);
        final boolean canManage = isCommentAuthor || isBookOwner || isAdmin;

        if (canManage) {
            holder.btnCommentAction.setVisibility(View.VISIBLE);
            holder.btnCommentAction.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onEdit(comment);
                }
            });
            holder.itemView.setOnLongClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onEdit(comment);
                    return true;
                }
                return false;
            });
        } else {
            holder.btnCommentAction.setVisibility(View.GONE);
            holder.itemView.setOnLongClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar, btnCommentAction;
        TextView tvUserName, tvOwnerBadge, tvTime, tvContent;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.img_comment_avatar);
            btnCommentAction = itemView.findViewById(R.id.btn_comment_action);
            tvUserName = itemView.findViewById(R.id.tv_comment_user_name);
            tvOwnerBadge = itemView.findViewById(R.id.tv_owner_badge);
            tvTime = itemView.findViewById(R.id.tv_comment_time);
            tvContent = itemView.findViewById(R.id.tv_comment_content);
        }
    }
}
