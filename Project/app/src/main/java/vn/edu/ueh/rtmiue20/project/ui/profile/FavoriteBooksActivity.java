package vn.edu.ueh.rtmiue20.project.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.ui.book.BookDetailActivity;
import vn.edu.ueh.rtmiue20.project.ui.favorite.FavoriteViewModel;
import vn.edu.ueh.rtmiue20.project.ui.home.BookAdapter;

public class FavoriteBooksActivity extends AppCompatActivity {

    private ImageView btnBack;
    private RecyclerView rvFavoriteBooks;
    private TextView tvEmpty;
    private BookAdapter bookAdapter;
    private FavoriteViewModel favoriteViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_books);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnBack = findViewById(R.id.btnBack);
        rvFavoriteBooks = findViewById(R.id.rvFavoriteBooks);
        tvEmpty = findViewById(R.id.tvEmpty);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        int columns = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 4 : 2;
        rvFavoriteBooks.setLayoutManager(new GridLayoutManager(this, columns));
        bookAdapter = new BookAdapter(book -> {
            Intent intent = new Intent(FavoriteBooksActivity.this, BookDetailActivity.class);
            intent.putExtra("bookId", book.getId());
            startActivity(intent);
        });
        rvFavoriteBooks.setAdapter(bookAdapter);

        favoriteViewModel = new ViewModelProvider(this).get(FavoriteViewModel.class);
        favoriteViewModel.getFavoritedBooksOffline().observe(this, books -> {
            if (books != null) {
                bookAdapter.update(books);
                if (books.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                }
            } else {
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }
}
