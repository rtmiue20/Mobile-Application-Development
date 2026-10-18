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
        
        h.tvTitle.setText(b.getTitle() != null ? b.getTitle() : "");
        h.tvAuthor.setText(b.getAuthor() != null ? b.getAuthor() : "");
        
        String category = b.getCategory();
        if (category == null || category.isEmpty()) {
            category = "Khác";
        }
        h.tvCategory.setText(category);
        
        String condition = b.getCondition();
        if (condition == null || condition.isEmpty()) {
            condition = "Mới";
        }
        h.tvCondition.setText(condition);
        
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
    }

    @Override public int getItemCount() { return books.size(); }

    static class BookHolder extends RecyclerView.ViewHolder {
        ImageView ivBookCover;
        TextView tvCategory, tvTitle, tvAuthor, tvCondition;
        
        BookHolder(View v) {
            super(v);
            ivBookCover = v.findViewById(R.id.ivBookCover);
            tvCategory = v.findViewById(R.id.tvCategory);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvAuthor = v.findViewById(R.id.tvAuthor);
            tvCondition = v.findViewById(R.id.tvCondition);
        }
    }
}
