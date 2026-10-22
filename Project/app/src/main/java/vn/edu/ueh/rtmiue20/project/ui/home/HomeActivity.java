package vn.edu.ueh.rtmiue20.project.ui.home;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.Book;
import vn.edu.ueh.rtmiue20.project.ui.book.BookDetailActivity;
import vn.edu.ueh.rtmiue20.project.ui.book.CreatePostActivity;
import vn.edu.ueh.rtmiue20.project.ui.profile.ProfileActivity;
import vn.edu.ueh.rtmiue20.project.ui.request.ManageRequestsActivity;
import vn.edu.ueh.rtmiue20.project.util.ViewPressEffectHelper;

public class HomeActivity extends AppCompatActivity {

    private BookAdapter bookAdapter;
    private List<Book> allBooksList = new ArrayList<>();
    private List<Book> currentFilteredList = new ArrayList<>();

    private ScrollView mainScrollView;
    private EditText etSearchBook;
    private ImageView btnClearSearch;
    private RecyclerView rvBooks;
    private TextView tvEmptyBooks;
    private TextView tvBookCountBadge;

    private ImageView btnNotifications;
    private ImageView btnGoToProfile;
    private View btnFilter;

    // Category pills
    private TextView pillAll, pillLiterature, pillEconomics, pillTextbook, pillOther;
    private String selectedCategory = "Tất cả";

    // Filter states
    private String filterCondition = "Tất cả";
    private String filterSharingType = "Tất cả";
    private String filterSortOrder = "Mới nhất";

    // Bottom Navigation & Actions
    private View btnNavHome, btnNavSearch, btnNavRequests, btnNavProfile;
    private ImageView fabAddBook;

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
        setupPressEffects();
    }

    private void initViews() {
        mainScrollView = findViewById(R.id.mainScrollView);
        etSearchBook = findViewById(R.id.etSearchBook);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        rvBooks = findViewById(R.id.rvBooks);
        tvEmptyBooks = findViewById(R.id.tvEmptyBooks);
        tvBookCountBadge = findViewById(R.id.tvBookCountBadge);

        btnNotifications = findViewById(R.id.btnNotifications);
        btnGoToProfile = findViewById(R.id.btnGoToProfile);
        btnFilter = findViewById(R.id.btnFilter);

        pillAll = findViewById(R.id.pillAll);
        pillLiterature = findViewById(R.id.pillLiterature);
        pillEconomics = findViewById(R.id.pillEconomics);
        pillTextbook = findViewById(R.id.pillTextbook);
        pillOther = findViewById(R.id.pillOther);

        btnNavHome = findViewById(R.id.btnNavHome);
        btnNavSearch = findViewById(R.id.btnNavSearch);
        btnNavRequests = findViewById(R.id.btnNavRequests);
        btnNavProfile = findViewById(R.id.btnNavProfile);
        fabAddBook = findViewById(R.id.fabAddBook);

        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        tvPageIndicator = findViewById(R.id.tvPageIndicator);
    }

    private void setupRecyclerView() {
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
        // Search text watcher
        etSearchBook.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(s.length() != 0 ? View.VISIBLE : View.GONE);
                currentPage = 1;
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearchBook.setText("");
            etSearchBook.clearFocus();
        });

        // Notifications Bell
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> showNotificationsDialog());
        }

        // Profile Buttons
        if (btnGoToProfile != null) {
            btnGoToProfile.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ProfileActivity.class)));
        }
        if (btnNavProfile != null) {
            btnNavProfile.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ProfileActivity.class)));
        }

        // FAB Add Book
        if (fabAddBook != null) {
            fabAddBook.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, CreatePostActivity.class)));
        }

        // Category Pills
        if (pillAll != null) pillAll.setOnClickListener(v -> selectCategory("Tất cả", pillAll));
        if (pillLiterature != null) pillLiterature.setOnClickListener(v -> selectCategory("Văn học", pillLiterature));
        if (pillEconomics != null) pillEconomics.setOnClickListener(v -> selectCategory("Kinh tế & Kỹ năng", pillEconomics));
        if (pillTextbook != null) pillTextbook.setOnClickListener(v -> selectCategory("Giáo trình", pillTextbook));
        if (pillOther != null) pillOther.setOnClickListener(v -> selectCategory("Khác", pillOther));

        // Filter Button
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilterDialog());
        }

        // Bottom Navigation Tabs
        if (btnNavHome != null) {
            btnNavHome.setOnClickListener(v -> {
                if (mainScrollView != null) {
                    mainScrollView.smoothScrollTo(0, 0);
                }
            });
        }

        if (btnNavSearch != null) {
            btnNavSearch.setOnClickListener(v -> {
                if (mainScrollView != null && etSearchBook != null) {
                    mainScrollView.smoothScrollTo(0, etSearchBook.getTop());
                    etSearchBook.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(etSearchBook, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            });
        }

        if (btnNavRequests != null) {
            btnNavRequests.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ManageRequestsActivity.class)));
        }

        // Pagination Buttons
        if (btnPrevPage != null) {
            btnPrevPage.setOnClickListener(v -> {
                if (currentPage > 1) {
                    currentPage--;
                    updatePagination();
                    if (mainScrollView != null) mainScrollView.smoothScrollTo(0, rvBooks.getTop());
                }
            });
        }

        if (btnNextPage != null) {
            btnNextPage.setOnClickListener(v -> {
                if (currentPage < totalPages) {
                    currentPage++;
                    updatePagination();
                    if (mainScrollView != null) mainScrollView.smoothScrollTo(0, rvBooks.getTop());
                }
            });
        }
    }

    private void setupPressEffects() {
        ViewPressEffectHelper.applyPressEffect(
                btnNotifications, btnGoToProfile, btnClearSearch,
                pillAll, pillLiterature, pillEconomics, pillTextbook, pillOther,
                btnFilter, btnPrevPage, btnNextPage,
                btnNavHome, btnNavSearch, fabAddBook, btnNavRequests, btnNavProfile
        );
    }

    private void selectCategory(String category, TextView selectedPill) {
        selectedCategory = category;
        TextView[] allPills = {pillAll, pillLiterature, pillEconomics, pillTextbook, pillOther};
        String[] baseTexts = {"Tất cả", "Văn học", "Kinh tế & Kỹ năng", "Giáo trình", "Khác"};

        for (int i = 0; i < allPills.length; i++) {
            TextView pill = allPills[i];
            if (pill == null) continue;
            if (pill == selectedPill) {
                pill.setBackgroundResource(R.drawable.bg_pill_active);
                pill.setTextColor(getResources().getColor(R.color.on_primary));
                pill.setText("✓ " + baseTexts[i]);
                pill.setTypeface(null, Typeface.BOLD);
            } else {
                pill.setBackgroundResource(R.drawable.bg_pill_inactive);
                pill.setTextColor(getResources().getColor(R.color.on_surface_variant));
                pill.setText(baseTexts[i]);
                pill.setTypeface(null, Typeface.NORMAL);
            }
        }
        currentPage = 1;
        applyFilters();
    }

    private void showNotificationsDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_notifications, null);
        dialog.setContentView(sheetView);

        ImageView btnClose = sheetView.findViewById(R.id.btnCloseNotifications);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        View btnViewRequests = sheetView.findViewById(R.id.btnViewRequestsFromNotification);
        if (btnViewRequests != null) {
            btnViewRequests.setOnClickListener(v -> {
                dialog.dismiss();
                startActivity(new Intent(HomeActivity.this, ManageRequestsActivity.class));
            });
        }

        ViewPressEffectHelper.applyPressEffect(btnClose, btnViewRequests);
        dialog.show();
    }

    private void showFilterDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_filter_books, null);
        dialog.setContentView(sheetView);

        ImageView btnClose = sheetView.findViewById(R.id.btnCloseFilter);
        TextView chipCondAll = sheetView.findViewById(R.id.chipCondAll);
        TextView chipCondNew = sheetView.findViewById(R.id.chipCondNew);
        TextView chipCondUsed = sheetView.findViewById(R.id.chipCondUsed);

        TextView chipTypeAll = sheetView.findViewById(R.id.chipTypeAll);
        TextView chipTypeFree = sheetView.findViewById(R.id.chipTypeFree);
        TextView chipTypeExchange = sheetView.findViewById(R.id.chipTypeExchange);

        TextView chipSortNewest = sheetView.findViewById(R.id.chipSortNewest);
        TextView chipSortOldest = sheetView.findViewById(R.id.chipSortOldest);
        TextView chipSortTitle = sheetView.findViewById(R.id.chipSortTitle);

        TextView btnReset = sheetView.findViewById(R.id.btnResetFilter);
        TextView btnApply = sheetView.findViewById(R.id.btnApplyFilter);

        final String[] tempCondition = {filterCondition};
        final String[] tempType = {filterSharingType};
        final String[] tempSort = {filterSortOrder};

        // Initialize chips UI according to current filter
        updateChipGroupState(tempCondition[0], "Tất cả", chipCondAll, "Còn mới / Rất mới", chipCondNew, "Đã qua sử dụng", chipCondUsed);
        updateChipGroupState(tempType[0], "Tất cả", chipTypeAll, "Tặng miễn phí", chipTypeFree, "Trao đổi", chipTypeExchange);
        updateChipGroupState(tempSort[0], "Mới nhất", chipSortNewest, "Cũ nhất", chipSortOldest, "Tên sách (A-Z)", chipSortTitle);

        // Chip click listeners
        if (chipCondAll != null) chipCondAll.setOnClickListener(v -> {
            tempCondition[0] = "Tất cả";
            setChipActive(chipCondAll, chipCondNew, chipCondUsed);
        });
        if (chipCondNew != null) chipCondNew.setOnClickListener(v -> {
            tempCondition[0] = "Còn mới / Rất mới";
            setChipActive(chipCondNew, chipCondAll, chipCondUsed);
        });
        if (chipCondUsed != null) chipCondUsed.setOnClickListener(v -> {
            tempCondition[0] = "Đã qua sử dụng";
            setChipActive(chipCondUsed, chipCondAll, chipCondNew);
        });

        if (chipTypeAll != null) chipTypeAll.setOnClickListener(v -> {
            tempType[0] = "Tất cả";
            setChipActive(chipTypeAll, chipTypeFree, chipTypeExchange);
        });
        if (chipTypeFree != null) chipTypeFree.setOnClickListener(v -> {
            tempType[0] = "Tặng miễn phí";
            setChipActive(chipTypeFree, chipTypeAll, chipTypeExchange);
        });
        if (chipTypeExchange != null) chipTypeExchange.setOnClickListener(v -> {
            tempType[0] = "Trao đổi";
            setChipActive(chipTypeExchange, chipTypeAll, chipTypeFree);
        });

        if (chipSortNewest != null) chipSortNewest.setOnClickListener(v -> {
            tempSort[0] = "Mới nhất";
            setChipActive(chipSortNewest, chipSortOldest, chipSortTitle);
        });
        if (chipSortOldest != null) chipSortOldest.setOnClickListener(v -> {
            tempSort[0] = "Cũ nhất";
            setChipActive(chipSortOldest, chipSortNewest, chipSortTitle);
        });
        if (chipSortTitle != null) chipSortTitle.setOnClickListener(v -> {
            tempSort[0] = "Tên sách (A-Z)";
            setChipActive(chipSortTitle, chipSortNewest, chipSortOldest);
        });

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                tempCondition[0] = "Tất cả";
                tempType[0] = "Tất cả";
                tempSort[0] = "Mới nhất";
                setChipActive(chipCondAll, chipCondNew, chipCondUsed);
                setChipActive(chipTypeAll, chipTypeFree, chipTypeExchange);
                setChipActive(chipSortNewest, chipSortOldest, chipSortTitle);
            });
        }

        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                filterCondition = tempCondition[0];
                filterSharingType = tempType[0];
                filterSortOrder = tempSort[0];
                dialog.dismiss();
                currentPage = 1;
                applyFilters();
            });
        }

        ViewPressEffectHelper.applyPressEffect(
                btnClose, btnReset, btnApply,
                chipCondAll, chipCondNew, chipCondUsed,
                chipTypeAll, chipTypeFree, chipTypeExchange,
                chipSortNewest, chipSortOldest, chipSortTitle
        );

        dialog.show();
    }

    private void updateChipGroupState(String currentVal, String val1, TextView chip1, String val2, TextView chip2, String val3, TextView chip3) {
        if (val2.equals(currentVal)) {
            setChipActive(chip2, chip1, chip3);
        } else if (val3.equals(currentVal)) {
            setChipActive(chip3, chip1, chip2);
        } else {
            setChipActive(chip1, chip2, chip3);
        }
    }

    private void setChipActive(TextView activeChip, TextView... otherChips) {
        if (activeChip != null) {
            activeChip.setBackgroundResource(R.drawable.bg_pill_active);
            activeChip.setTextColor(getResources().getColor(R.color.on_primary));
            activeChip.setTypeface(null, Typeface.BOLD);
        }
        for (TextView other : otherChips) {
            if (other != null) {
                other.setBackgroundResource(R.drawable.bg_pill_inactive);
                other.setTextColor(getResources().getColor(R.color.on_surface_variant));
                other.setTypeface(null, Typeface.NORMAL);
            }
        }
    }

    private void applyFilters() {
        if (allBooksList == null) return;

        currentFilteredList = new ArrayList<>();
        String searchQuery = etSearchBook != null ? etSearchBook.getText().toString().trim().toLowerCase() : "";

        for (Book b : allBooksList) {
            // 1. Check status hidden
            String status = b.getStatus();
            if ("hidden".equalsIgnoreCase(status)) {
                continue;
            }

            // 2. Category Filter
            if (!"Tất cả".equals(selectedCategory)) {
                String bookCategory = b.getCategory() != null ? b.getCategory().toLowerCase() : "";
                if ("Văn học".equals(selectedCategory)) {
                    if (!bookCategory.contains("văn học") && !bookCategory.contains("tiểu thuyết") && !bookCategory.contains("truyện")) {
                        continue;
                    }
                } else if ("Kinh tế & Kỹ năng".equals(selectedCategory)) {
                    if (!bookCategory.contains("kinh tế") && !bookCategory.contains("kỹ năng")) {
                        continue;
                    }
                } else if ("Giáo trình".equals(selectedCategory)) {
                    if (!bookCategory.contains("giáo trình") && !bookCategory.contains("học thuật")) {
                        continue;
                    }
                } else if ("Khác".equals(selectedCategory)) {
                    if (bookCategory.contains("văn học") || bookCategory.contains("kinh tế")
                            || bookCategory.contains("kỹ năng") || bookCategory.contains("giáo trình")) {
                        continue;
                    }
                }
            }

            // 3. Condition Filter
            if (!"Tất cả".equals(filterCondition)) {
                String bookCond = b.getCondition() != null ? b.getCondition().toLowerCase() : "";
                if ("Còn mới / Rất mới".equals(filterCondition)) {
                    if (!bookCond.contains("mới") && !bookCond.contains("new") && !bookCond.contains("99") && !bookCond.contains("98")) {
                        continue;
                    }
                } else if ("Đã qua sử dụng".equals(filterCondition)) {
                    if (bookCond.contains("mới") && !bookCond.contains("cũ") && !bookCond.contains("đã qua")) {
                        continue;
                    }
                }
            }

            // 4. Sharing Type Filter
            if (!"Tất cả".equals(filterSharingType)) {
                String bookStatus = b.getStatus() != null ? b.getStatus().toLowerCase() : "available";
                if ("Tặng miễn phí".equals(filterSharingType)) {
                    if (!"available".equals(bookStatus) && !bookStatus.contains("miễn phí")) {
                        continue;
                    }
                } else if ("Trao đổi".equals(filterSharingType)) {
                    if (!"exchanging".equals(bookStatus) && !bookStatus.contains("trao đổi")) {
                        continue;
                    }
                }
            }

            // 5. Search Text Query (Title, Author, Category, Description)
            if (!searchQuery.isEmpty()) {
                String title = b.getTitle() != null ? b.getTitle().toLowerCase() : "";
                String author = b.getAuthor() != null ? b.getAuthor().toLowerCase() : "";
                String category = b.getCategory() != null ? b.getCategory().toLowerCase() : "";
                String desc = b.getDescription() != null ? b.getDescription().toLowerCase() : "";

                if (!title.contains(searchQuery) && !author.contains(searchQuery)
                        && !category.contains(searchQuery) && !desc.contains(searchQuery)) {
                    continue;
                }
            }

            currentFilteredList.add(b);
        }

        // 6. Sorting
        if ("Cũ nhất".equals(filterSortOrder)) {
            currentFilteredList.sort((b1, b2) -> Long.compare(b1.getCreatedAt(), b2.getCreatedAt()));
        } else if ("Tên sách (A-Z)".equals(filterSortOrder)) {
            currentFilteredList.sort((b1, b2) -> {
                String t1 = b1.getTitle() != null ? b1.getTitle() : "";
                String t2 = b2.getTitle() != null ? b2.getTitle() : "";
                return t1.compareToIgnoreCase(t2);
            });
        } else {
            // Default: Mới nhất
            currentFilteredList.sort((b1, b2) -> Long.compare(b2.getCreatedAt(), b1.getCreatedAt()));
        }

        // Update count badge
        if (tvBookCountBadge != null) {
            tvBookCountBadge.setText(currentFilteredList.size() + " cuốn");
        }

        // Calculate total pages
        totalPages = (int) Math.ceil((double) currentFilteredList.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        updatePagination();
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
