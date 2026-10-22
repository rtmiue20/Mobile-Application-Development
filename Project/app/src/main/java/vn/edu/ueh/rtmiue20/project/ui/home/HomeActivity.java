package vn.edu.ueh.rtmiue20.project.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.Book;
import vn.edu.ueh.rtmiue20.project.ui.book.BookDetailActivity;
import vn.edu.ueh.rtmiue20.project.ui.book.CreatePostActivity;
import vn.edu.ueh.rtmiue20.project.ui.profile.ProfileActivity;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private BookAdapter bookAdapter;
    private List<Book> allBooksList = new ArrayList<>();
    private List<Book> currentFilteredList = new ArrayList<>();

    private EditText etSearchBook;
    private ImageView btnClearSearch;
    private RecyclerView rvBooks;
    private TextView tvEmptyBooks;
    
    // Pagination
    private static final int ITEMS_PER_PAGE = 4;
    private int currentPage = 1;
    private int totalPages = 1;
    private TextView btnPrevPage, btnNextPage, tvPageIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_member_dashboard);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainMemberDashboard), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // SỬA SỐ 0 THÀNH systemBars.bottom
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupRecyclerView();

        BookViewModel bookViewModel = new ViewModelProvider(this).get(BookViewModel.class);
        bookViewModel.getAllBooks().observe(this, books -> {
            if (books != null) {
                allBooksList = books;
                applyFilters();
            }
        });

        setupListeners();
    }

    private void initViews() {
        etSearchBook = findViewById(R.id.etSearchBook);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        rvBooks = findViewById(R.id.rvBooks);
        tvEmptyBooks = findViewById(R.id.tvEmptyBooks);
        
        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        tvPageIndicator = findViewById(R.id.tvPageIndicator);

        ImageView btnGoToProfile = findViewById(R.id.btnGoToProfile);
        if (btnGoToProfile != null) {
            btnGoToProfile.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ProfileActivity.class)));
        }

        ImageView fabAddBook = findViewById(R.id.fabAddBook);
        if (fabAddBook != null) {
            fabAddBook.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, CreatePostActivity.class)));
        }

        View btnNavProfile = findViewById(R.id.btnNavProfile);
        if (btnNavProfile != null) {
            btnNavProfile.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ProfileActivity.class)));
        }
    }

    private void setupRecyclerView() {
        // Handle landscape responsiveness
        int columns = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 4 : 2;
        rvBooks.setLayoutManager(new GridLayoutManager(this, columns));
        bookAdapter = new BookAdapter(book -> {
            Intent intent = new Intent(HomeActivity.this, BookDetailActivity.class);
            intent.putExtra("bookId", book.getId());
            startActivity(intent);
        });
        rvBooks.setAdapter(bookAdapter);
    }

    private void setupListeners() {
        etSearchBook.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(s.length() != 0 ? View.VISIBLE : View.GONE);
                currentPage = 1; // Reset to page 1 on search
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearchBook.setText("");
            etSearchBook.clearFocus();
        });
        
        btnPrevPage.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                updatePagination();
            }
        });

        btnNextPage.setOnClickListener(v -> {
            if (currentPage < totalPages) {
                currentPage++;
                updatePagination();
            }
        });
    }

    private void applyFilters() {
        if (allBooksList == null) return;

        currentFilteredList = new ArrayList<>();
        String searchQuery = etSearchBook.getText().toString().trim().toLowerCase();

        for (Book b : allBooksList) {
            // 1. Status
            String status = b.getStatus();
            if ("hidden".equals(status)) {
                continue;
            }

            // 2. Search
            if (!searchQuery.isEmpty()) {
                String title = b.getTitle() != null ? b.getTitle().toLowerCase() : "";
                String author = b.getAuthor() != null ? b.getAuthor().toLowerCase() : "";
                if (!title.contains(searchQuery) && !author.contains(searchQuery)) {
                    continue;
                }
            }

            currentFilteredList.add(b);
        }

        // Default Sorting (Mới nhất)
        currentFilteredList.sort((b1, b2) -> Long.compare(b2.getCreatedAt(), b1.getCreatedAt()));

        // Calculate total pages
        totalPages = (int) Math.ceil((double) currentFilteredList.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        updatePagination();
        
        TextView tvSectionTitleCount = findViewById(R.id.tvSectionTitle); // Will need a specific ID if we update count dynamically
        // Just leaving the UI string updates simplified for now
    }
    
    private void updatePagination() {
        if (currentFilteredList.isEmpty()) {
            tvEmptyBooks.setVisibility(View.VISIBLE);
            rvBooks.setVisibility(View.GONE);
            findViewById(R.id.layoutPagination).setVisibility(View.GONE);
            return;
        }
        
        tvEmptyBooks.setVisibility(View.GONE);
        rvBooks.setVisibility(View.VISIBLE);
        findViewById(R.id.layoutPagination).setVisibility(View.VISIBLE);

        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, currentFilteredList.size());
        
        List<Book> pageItems = currentFilteredList.subList(startIndex, endIndex);
        bookAdapter.update(pageItems);
        
        tvPageIndicator.setText(currentPage + " / " + totalPages);
        
        btnPrevPage.setVisibility(currentPage > 1 ? View.VISIBLE : View.INVISIBLE);
        btnNextPage.setVisibility(currentPage < totalPages ? View.VISIBLE : View.INVISIBLE);
    }
}
