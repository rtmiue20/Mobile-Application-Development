package vn.edu.ueh.rtmiue20.project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        h.title.setText(b.getTitle());
        h.author.setText(b.getAuthor());
        h.itemView.setOnClickListener(v -> listener.onClick(b));
    }

    @Override public int getItemCount() { return books.size(); }

    static class BookHolder extends RecyclerView.ViewHolder {
        TextView title, author;
        BookHolder(View v) {
            super(v);
            title = v.findViewById(R.id.tvTitle);
            author = v.findViewById(R.id.tvAuthor);
        }
    }
}
