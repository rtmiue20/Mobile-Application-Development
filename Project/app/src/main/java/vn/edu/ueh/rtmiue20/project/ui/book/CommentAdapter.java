package vn.edu.ueh.rtmiue20.project.ui.book;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.Comment;

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
        final Comment comment = comments.get(position);

        holder.tvUserName.setText(comment.getUserName());
        holder.tvContent.setText(comment.getContent());

        Long createdAt = comment.getCreatedAt();
        if (createdAt != null) {
            CharSequence relativeTime = android.text.format.DateUtils.getRelativeTimeSpanString(
                    createdAt,
                    System.currentTimeMillis(),
                    android.text.format.DateUtils.MINUTE_IN_MILLIS);
            if (comment.getUpdatedAt() != null) {
                holder.tvTime.setText(relativeTime + " (đã chỉnh sửa)");
            } else {
                holder.tvTime.setText(relativeTime);
            }
        } else {
            holder.tvTime.setText("");
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        final boolean isOwner = currentUser != null && currentUser.getUid().equals(comment.getUserId());

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (isOwner && actionListener != null) {
                    actionListener.onEdit(comment);
                    return true;
                }
                return false;
            }
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
