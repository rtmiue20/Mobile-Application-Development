package vn.edu.ueh.rtmiue20.project.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.Book;
import vn.edu.ueh.rtmiue20.project.ui.book.BookDetailActivity;
import vn.edu.ueh.rtmiue20.project.ui.book.CreatePostActivity;
import vn.edu.ueh.rtmiue20.project.ui.profile.ProfileActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private BookViewModel bookViewModel;
    private BookAdapter bookAdapter;
    private List<Book> allBooksList = new ArrayList<>();

    private EditText etSearchBook;
    private ImageView btnClearSearch;
    private Spinner spinnerCategory, spinnerCondition, spinnerSort;
    private RecyclerView rvBooks;
    private TextView tvEmptyBooks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainHome), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupSpinners();
        setupRecyclerView();

        bookViewModel = new ViewModelProvider(this).get(BookViewModel.class);
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
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerCondition = findViewById(R.id.spinnerCondition);
        spinnerSort = findViewById(R.id.spinnerSort);
        rvBooks = findViewById(R.id.rvBooks);
        tvEmptyBooks = findViewById(R.id.tvEmptyBooks);

        ImageView btnGoToProfile = findViewById(R.id.btnGoToProfile);
        btnGoToProfile.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
        });

        FloatingActionButton fabAddBook = findViewById(R.id.fabAddBook);
        fabAddBook.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, CreatePostActivity.class));
        });

        View btnNavProfile = findViewById(R.id.btnNavProfile);
        if (btnNavProfile != null) {
            btnNavProfile.setOnClickListener(v -> {
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
            });
        }
    }

    private void setupSpinners() {
        String[] categories = {"Tất cả", "Giáo trình", "Văn học", "Kỹ năng", "Kinh tế", "Khác"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        String[] conditions = {"Tất cả tình trạng", "Mới", "Khá mới", "Cũ"};
        ArrayAdapter<String> condAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, conditions);
        condAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCondition.setAdapter(condAdapter);

        String[] sorts = {"Mới nhất", "Tên A-Z"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sorts);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(sortAdapter);
    }

    private void setupRecyclerView() {
        int columns = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 3 : 2;
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
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearchBook.setText("");
            etSearchBook.clearFocus();
        });

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerCategory.setOnItemSelectedListener(filterListener);
        spinnerCondition.setOnItemSelectedListener(filterListener);
        spinnerSort.setOnItemSelectedListener(filterListener);
    }

    private void applyFilters() {
        if (allBooksList == null) return;

        List<Book> filteredList = new ArrayList<>();
        String searchQuery = etSearchBook.getText().toString().trim().toLowerCase();
        String selCategory = spinnerCategory.getSelectedItem() != null ? spinnerCategory.getSelectedItem().toString() : "Tất cả";
        String selCondition = spinnerCondition.getSelectedItem() != null ? spinnerCondition.getSelectedItem().toString() : "Tất cả tình trạng";
        String selSort = spinnerSort.getSelectedItem() != null ? spinnerSort.getSelectedItem().toString() : "Mới nhất";

        for (Book b : allBooksList) {
            // 1. Status
            String status = b.getStatus();
            if ("hidden".equals(status)) {
                continue;
            }

            // 2. Search
            boolean matchesSearch = true;
            if (!searchQuery.isEmpty()) {
                String title = b.getTitle() != null ? b.getTitle().toLowerCase() : "";
                String author = b.getAuthor() != null ? b.getAuthor().toLowerCase() : "";
                if (!title.contains(searchQuery) && !author.contains(searchQuery)) {
                    matchesSearch = false;
                }
            }
            if (!matchesSearch) continue;

            // 3. Category
            if (!selCategory.equals("Tất cả")) {
                String cat = b.getCategory() != null && !b.getCategory().isEmpty() ? b.getCategory() : "Khác";
                if (!cat.equals(selCategory)) {
                    continue;
                }
            }

            // 4. Condition
            if (!selCondition.equals("Tất cả tình trạng")) {
                String cond = b.getCondition() != null && !b.getCondition().isEmpty() ? b.getCondition() : "Mới";
                if (!cond.equals(selCondition)) {
                    continue;
                }
            }

            filteredList.add(b);
        }

        // 5. Sorting
        if ("Tên A-Z".equals(selSort)) {
            Collections.sort(filteredList, (b1, b2) -> {
                String t1 = b1.getTitle() != null ? b1.getTitle() : "";
                String t2 = b2.getTitle() != null ? b2.getTitle() : "";
                return t1.compareToIgnoreCase(t2);
            });
        } else {
            // "Mới nhất" (giảm dần theo createdAt)
            Collections.sort(filteredList, (b1, b2) -> Long.compare(b2.getCreatedAt(), b1.getCreatedAt()));
        }

        bookAdapter.update(filteredList);

        if (filteredList.isEmpty()) {
            tvEmptyBooks.setVisibility(View.VISIBLE);
            rvBooks.setVisibility(View.GONE);
        } else {
            tvEmptyBooks.setVisibility(View.GONE);
            rvBooks.setVisibility(View.VISIBLE);
        }
    }
}
