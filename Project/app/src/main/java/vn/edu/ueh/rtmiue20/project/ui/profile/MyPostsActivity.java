package vn.edu.ueh.rtmiue20.project.ui.profile;

import android.content.Intent;
import android.content.SharedPreferences;
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
import vn.edu.ueh.rtmiue20.project.ui.home.BookViewModel;
import vn.edu.ueh.rtmiue20.project.util.LocationHelper;

public class MyPostsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private RecyclerView rvMyPosts;
    private TextView tvEmpty;
    private BookAdapter bookAdapter;
    private String currentUserId;
    private BookViewModel bookViewModel;
    private FavoriteViewModel favoriteViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_posts);

        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = user.getUid();

        btnBack = findViewById(R.id.btnBack);
        rvMyPosts = findViewById(R.id.rvMyPosts);
        tvEmpty = findViewById(R.id.tvEmpty);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        int columns = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 4 : 2;
        rvMyPosts.setLayoutManager(new GridLayoutManager(this, columns));
        bookAdapter = new BookAdapter(book -> {
            Intent intent = new Intent(MyPostsActivity.this, BookDetailActivity.class);
            intent.putExtra("bookId", book.getId());
            startActivity(intent);
        });
        rvMyPosts.setAdapter(bookAdapter);

        SharedPreferences prefs = getSharedPreferences("BookSharePrefs", MODE_PRIVATE);
        float lat = prefs.getFloat("user_lat", 0f);
        float lng = prefs.getFloat("user_lng", 0f);
        if (lat != 0f && lng != 0f) {
            bookAdapter.setUserDistanceToExchange(LocationHelper.getDistanceToUEHCampusB(lat, lng));
        }

        bookViewModel = new ViewModelProvider(this).get(BookViewModel.class);
        favoriteViewModel = new ViewModelProvider(this).get(FavoriteViewModel.class);

        loadMyPosts();
    }

    private void loadMyPosts() {
        // Đồng bộ qua Room SQLite + Firestore để hoạt động mượt cả khi Offline (FR12)
        bookViewModel.getBooksByOwner(currentUserId).observe(this, list -> {
            if (list != null) {
                bookAdapter.update(list);
                if (list.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                }
            }
        });

        favoriteViewModel.getFavoriteBookIds().observe(this, ids -> {
            if (bookAdapter != null && ids != null) {
                bookAdapter.setFavoritedBookIds(ids);
            }
        });
    }
}
