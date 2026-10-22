package vn.edu.ueh.rtmiue20.project.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.Book;

import java.util.ArrayList;
import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookHolder> {
    public interface OnBookClick { void onClick(Book book); }

    private List<Book> books = new ArrayList<>();
    private final OnBookClick listener;

    public BookAdapter(OnBookClick listener) { this.listener = listener; }

    public void update(List<Book> list) {
        books = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book, parent, false);
        return new BookHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BookHolder h, int position) {
        Book b = books.get(position);
        
        h.tvBookTitle.setText(b.getTitle() != null ? b.getTitle() : "");
        h.tvBookAuthor.setText(b.getAuthor() != null ? b.getAuthor() : "");
        
        String condition = b.getCondition();
        if (condition == null || condition.isEmpty()) {
            condition = "Mới";
        }
        h.tvCondition.setText(condition);
        
        String status = b.getStatus();
        if ("available".equals(status)) {
            h.tvExchangeType.setText("Miễn phí");
        } else if ("exchanging".equals(status)) {
            h.tvExchangeType.setText("Đang trao đổi");
        } else if (status != null && !status.isEmpty()) {
            h.tvExchangeType.setText(status);
        } else {
            h.tvExchangeType.setText("Miễn phí");
        }
        
        String ownerId = b.getOwnerId();
        if (ownerId != null && !ownerId.trim().isEmpty()) {
            String displayName = ownerId;
            if (displayName.contains("@")) {
                displayName = displayName.substring(0, displayName.indexOf("@"));
            }
            h.tvUserName.setText(displayName);
            if (!displayName.isEmpty()) {
                h.tvUserAvatar.setText(String.valueOf(displayName.charAt(0)).toUpperCase());
            } else {
                h.tvUserAvatar.setText("U");
            }
        } else {
            h.tvUserName.setText("Hà Linh");
            h.tvUserAvatar.setText("H");
        }

        if (b.getImageUrl() == null || b.getImageUrl().isEmpty()) {
            Glide.with(h.itemView.getContext())
                 .load(R.drawable.ic_bookshare_logo)
                 .into(h.ivBookCover);
        } else {
            Glide.with(h.itemView.getContext())
                 .load(b.getImageUrl())
                 .placeholder(R.drawable.ic_bookshare_logo)
                 .error(R.drawable.ic_bookshare_logo)
                 .into(h.ivBookCover);
        }

        h.itemView.setOnClickListener(v -> listener.onClick(b));
        vn.edu.ueh.rtmiue20.project.util.ViewPressEffectHelper.applyPressEffect(h.itemView, h.btnFavorite);
        
        // Handle favorite toggle (visual only for now)
        h.btnFavorite.setOnClickListener(v -> {
            boolean isFav = (v.getTag() != null) && (boolean) v.getTag();
            if (isFav) {
                h.btnFavorite.setImageResource(R.drawable.ic_favorite_border);
                h.btnFavorite.setColorFilter(v.getContext().getResources().getColor(R.color.outline));
                v.setTag(false);
            } else {
                h.btnFavorite.setImageResource(R.drawable.ic_favorite_border); // would be filled heart
                h.btnFavorite.setColorFilter(v.getContext().getResources().getColor(R.color.error));
                v.setTag(true);
            }
        });
    }

    @Override public int getItemCount() { return books.size(); }

    static class BookHolder extends RecyclerView.ViewHolder {
        ImageView ivBookCover;
        ImageView btnFavorite;
        TextView tvBookTitle, tvBookAuthor, tvCondition;
        TextView tvUserAvatar, tvUserName, tvExchangeType;
        
        BookHolder(View v) {
            super(v);
            ivBookCover = v.findViewById(R.id.ivBookCover);
            btnFavorite = v.findViewById(R.id.btnFavorite);
            tvBookTitle = v.findViewById(R.id.tvBookTitle);
            tvBookAuthor = v.findViewById(R.id.tvBookAuthor);
            tvCondition = v.findViewById(R.id.tvCondition);
            tvUserAvatar = v.findViewById(R.id.tvUserAvatar);
            tvUserName = v.findViewById(R.id.tvUserName);
            tvExchangeType = v.findViewById(R.id.tvExchangeType);
        }
    }
}
