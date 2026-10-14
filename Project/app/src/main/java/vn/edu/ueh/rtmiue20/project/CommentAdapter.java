package vn.edu.ueh.rtmiue20.project;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> comments = new ArrayList<Comment>();

    public void setComments(List<Comment> comments) {
        if (comments == null) {
            this.comments = new ArrayList<Comment>();
        } else {
            this.comments = comments;
        }
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
            holder.tvTime.setText(relativeTime);
        } else {
            holder.tvTime.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {

        TextView tvUserName;
        TextView tvTime;
        TextView tvContent;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_comment_user_name);
            tvTime = itemView.findViewById(R.id.tv_comment_time);
            tvContent = itemView.findViewById(R.id.tv_comment_content);
        }
    }
}