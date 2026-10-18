package vn.edu.ueh.rtmiue20.project;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    public interface OnCommentActionListener {
        void onEdit(Comment comment);
        void onDelete(Comment comment);
    }

    private List<Comment> comments = new ArrayList<>();
    private final OnCommentActionListener actionListener;

    public CommentAdapter(OnCommentActionListener listener) {
        this.actionListener = listener;
    }

    public void setComments(List<Comment> comments) {
        this.comments = (comments == null) ? new ArrayList<>() : comments;
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
        Comment comment = comments.get(position);

        holder.tvUserName.setText(comment.getUserName());
        holder.tvContent.setText(comment.getContent());

        Long createdAt = comment.getCreatedAt();
        if (createdAt != null) {
            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    createdAt,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS);
            if (comment.getUpdatedAt() != null) {
                holder.tvTime.setText(relativeTime + " (đã chỉnh sửa)");
            } else {
                holder.tvTime.setText(relativeTime);
            }
        } else {
            holder.tvTime.setText("");
        }

        // Kiểm tra quyền: Người dùng hiện tại có phải chủ nhân bình luận không
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean isOwner = currentUser != null && currentUser.getUid().equals(comment.getUserId());

        // Nhấn giữ hoặc nhấn vào bình luận của mình để hiện tùy chọn Sửa / Xóa
        holder.itemView.setOnLongClickListener(v -> {
            if (isOwner && actionListener != null) {
                actionListener.onEdit(comment); // Kích hoạt dialog lựa chọn
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvTime, tvContent;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_comment_user_name);
            tvTime = itemView.findViewById(R.id.tv_comment_time);
            tvContent = itemView.findViewById(R.id.tv_comment_content);
        }
    }
}