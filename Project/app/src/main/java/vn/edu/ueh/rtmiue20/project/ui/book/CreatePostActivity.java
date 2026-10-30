package vn.edu.ueh.rtmiue20.project.ui.book;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.Book;
import vn.edu.ueh.rtmiue20.project.data.repository.AuthRepository;
import vn.edu.ueh.rtmiue20.project.data.repository.BookRepository;
import vn.edu.ueh.rtmiue20.project.ui.home.BookViewModel;
import vn.edu.ueh.rtmiue20.project.util.LocationHelper;

public class CreatePostActivity extends AppCompatActivity {

    private ImageView btnCancel, ivPreviewImage, ivMapLocation;
    private TextView tvHeaderCreatePost;
    private MaterialCardView cardAddImage;
    private View layoutAddImagePlaceholder, layoutExchangeContainer, layoutFeeLocationContainer;
    private EditText etTitle, etAuthor, etDescription, etConditionOther;
    private AutoCompleteTextView etExchangeBookTitle, etExchangeBookAuthor;
    private EditText etPrice;
    private Spinner spinnerCategory, spCity, spDistrict, spWard;
    private RadioGroup rgCondition, rgSharingType;
    private Button btnSubmit;

    private Uri selectedImageUri = null;
    private BookViewModel bookViewModel;
    private AuthRepository authRepository;

    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    // Chế độ chỉnh sửa sách đã đăng
    private String editBookId = null;
    private Book existingBook = null;
    private boolean hasLoadedExistingBook = false;
    private boolean isRestoringLocation = false;

    // Quản lý việc chọn ảnh và hiển thị preview
    private final ActivityResultLauncher<String[]> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;

                    // Ẩn giao diện chữ, hiện thẻ ImageView
                    layoutAddImagePlaceholder.setVisibility(View.GONE);
                    ivPreviewImage.setVisibility(View.VISIBLE);

                    Glide.with(this)
                            .load(uri)
                            .centerCrop()
                            .into(ivPreviewImage);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_post);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainCreatePost), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bookViewModel = new ViewModelProvider(this).get(BookViewModel.class);
        authRepository = AuthRepository.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        editBookId = getIntent().getStringExtra("edit_book_id");

        initViews();
        setupSpinner();
        setupCityDistrictWardSpinners();
        setupLocationPermissionLauncher();
        setupExchangeBookAutocomplete();
        setupListeners();

        if (editBookId != null && !editBookId.trim().isEmpty()) {
            loadBookForEditing(editBookId);
        }
    }

    private void initViews() {
        btnCancel = findViewById(R.id.btnCancel);
        tvHeaderCreatePost = findViewById(R.id.tvHeaderCreatePost);
        cardAddImage = findViewById(R.id.cardAddImage);
        layoutAddImagePlaceholder = findViewById(R.id.layoutAddImagePlaceholder);
        ivPreviewImage = findViewById(R.id.ivPreviewImage);
        etTitle = findViewById(R.id.etTitle);
        etAuthor = findViewById(R.id.etAuthor);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        rgCondition = findViewById(R.id.rgCondition);
        rgSharingType = findViewById(R.id.rgSharingType);
        etDescription = findViewById(R.id.etDescription);
        btnSubmit = findViewById(R.id.btnSubmit);
        etConditionOther = findViewById(R.id.etConditionOther);

        layoutExchangeContainer = findViewById(R.id.layoutExchangeContainer);
        etExchangeBookTitle = findViewById(R.id.etExchangeBookTitle);
        etExchangeBookAuthor = findViewById(R.id.etExchangeBookAuthor);

        layoutFeeLocationContainer = findViewById(R.id.layoutFeeLocationContainer);
        etPrice = findViewById(R.id.etPrice);
        ivMapLocation = findViewById(R.id.ivMapLocation);
        spCity = findViewById(R.id.spCity);
        spDistrict = findViewById(R.id.spDistrict);
        spWard = findViewById(R.id.spWard);
    }

    /**
     * Tách bỏ phần thông tin giao dịch (Hình thức, Giá, Vị trí, Sách muốn đổi)
     * vốn được append tự động vào cuối description khi lưu.
     * Tránh triệt để lỗi nhân bản nội dung (Text Duplication) khi mở form chỉnh sửa bài đăng.
     */
    public static String stripAppendedMetadata(String desc) {
        if (desc == null || desc.trim().isEmpty()) {
            return "";
        }
        String normalized = desc.replace("\r\n", "\n").replace("\r", "\n");
        Pattern pattern = Pattern.compile(
                "(?i)(?:^|\\n)\\s*(Hình thức:|- Sách muốn đổi:|- Giá:|- Vị trí:)"
        );
        Matcher matcher = pattern.matcher(normalized);
        if (matcher.find()) {
            return normalized.substring(0, matcher.start()).trim();
        }
        return normalized.trim();
    }

    private void loadBookForEditing(String id) {
        if (tvHeaderCreatePost != null) {
            tvHeaderCreatePost.setText("Chỉnh sửa thông tin sách");
        }
        if (btnSubmit != null) {
            btnSubmit.setText("Lưu cập nhật");
        }

        bookViewModel.getBook(id).observe(this, book -> {
            if (book != null && !hasLoadedExistingBook) {
                hasLoadedExistingBook = true;
                existingBook = book;

                if (book.getTitle() != null) etTitle.setText(book.getTitle());
                if (book.getAuthor() != null) etAuthor.setText(book.getAuthor());

                String desc = book.getDescription() != null ? book.getDescription() : "";
                // Chỉ lấy nội dung mô tả thực tế của người dùng, loại bỏ metadata giao dịch đã bị append
                String cleanDesc = stripAppendedMetadata(desc);
                etDescription.setText(cleanDesc);

                if (book.getImageUrl() != null && !book.getImageUrl().isEmpty()) {
                    layoutAddImagePlaceholder.setVisibility(View.GONE);
                    ivPreviewImage.setVisibility(View.VISIBLE);
                    Glide.with(this)
                            .load(book.getImageUrl())
                            .centerCrop()
                            .into(ivPreviewImage);
                }

                List<String> categories = LocationHelper.getBookCategories();
                if (book.getCategory() != null) {
                    int catIdx = categories.indexOf(book.getCategory());
                    if (catIdx >= 0 && spinnerCategory != null) {
                        spinnerCategory.setSelection(catIdx);
                    }
                }

                String cond = book.getCondition() != null ? book.getCondition() : "Mới 99%";
                if ("Mới 99%".equalsIgnoreCase(cond)) {
                    rgCondition.check(R.id.rbConditionNew);
                } else if ("Tốt".equalsIgnoreCase(cond)) {
                    rgCondition.check(R.id.rbConditionGood);
                } else if ("Cũ vừa".equalsIgnoreCase(cond)) {
                    rgCondition.check(R.id.rbConditionOld);
                } else {
                    rgCondition.check(R.id.rbConditionOther);
                    etConditionOther.setVisibility(View.VISIBLE);
                    etConditionOther.setText(cond);
                }

                if ("exchanging".equalsIgnoreCase(book.getStatus()) || desc.contains("Trao đổi sách khác") || desc.contains("Đổi sách")) {
                    rgSharingType.check(R.id.rbShareExchange);
                    Matcher exMatcher = Pattern.compile(
                            "(?i)-\\s*Sách muốn đổi:\\s*([^(]+?)(?:\\s*\\(Tác giả:\\s*([^)]+)\\))?(?:\\r?\\n|$)"
                    ).matcher(desc);
                    if (exMatcher.find()) {
                        String exT = exMatcher.group(1);
                        String exA = exMatcher.group(2);
                        if (exT != null && !exT.trim().isEmpty() && !"Sách cùng thể loại".equalsIgnoreCase(exT.trim())) {
                            if (etExchangeBookTitle != null) etExchangeBookTitle.setText(exT.trim());
                        }
                        if (exA != null && !exA.trim().isEmpty() && !"Tùy chọn".equalsIgnoreCase(exA.trim())) {
                            if (etExchangeBookAuthor != null) etExchangeBookAuthor.setText(exA.trim());
                        }
                    }
                } else if (desc.contains("Phí tượng trưng")) {
                    rgSharingType.check(R.id.rbShareFee);
                    Matcher priceMatcher = Pattern.compile(
                            "(?i)-\\s*Giá:\\s*([0-9.,]+)"
                    ).matcher(desc);
                    if (priceMatcher.find()) {
                        String p = priceMatcher.group(1);
                        if (p != null && etPrice != null) {
                            etPrice.setText(p.trim());
                        }
                    }

                    Matcher locMatcher = Pattern.compile(
                            "(?i)-\\s*Vị trí:\\s*([^\r\n]+)"
                    ).matcher(desc);
                    if (locMatcher.find()) {
                        String locStr = locMatcher.group(1);
                        if (locStr != null) {
                            String[] parts = locStr.split(",");
                            if (parts.length >= 3) {
                                String targetWard = parts[0].trim();
                                String targetDistrict = parts[1].trim();
                                String targetCity = parts[2].trim();
                                restoreLocationSelection(targetCity, targetDistrict, targetWard);
                            }
                        }
                    }
                } else {
                    rgSharingType.check(R.id.rbShareFree);
                }
            }
        });
    }

    private void setupSpinner() {
        List<String> categories = LocationHelper.getBookCategories();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupExchangeBookAutocomplete() {
        bookViewModel.getAllBooks().observe(this, books -> {
            if (books != null && !books.isEmpty()) {
                BookSuggestionAdapter adapterTitle = new BookSuggestionAdapter(this, books);
                etExchangeBookTitle.setAdapter(adapterTitle);

                BookSuggestionAdapter adapterAuthor = new BookSuggestionAdapter(this, books);
                etExchangeBookAuthor.setAdapter(adapterAuthor);
            }
        });

        etExchangeBookTitle.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getItemAtPosition(position);
            if (item instanceof Book) {
                Book selected = (Book) item;
                etExchangeBookTitle.setText(selected.getTitle() != null ? selected.getTitle() : "");
                etExchangeBookAuthor.setText(selected.getAuthor() != null ? selected.getAuthor() : "");
                etExchangeBookTitle.setSelection(etExchangeBookTitle.getText().length());
            }
        });

        etExchangeBookAuthor.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getItemAtPosition(position);
            if (item instanceof Book) {
                Book selected = (Book) item;
                etExchangeBookTitle.setText(selected.getTitle() != null ? selected.getTitle() : "");
                etExchangeBookAuthor.setText(selected.getAuthor() != null ? selected.getAuthor() : "");
                etExchangeBookAuthor.setSelection(etExchangeBookAuthor.getText().length());
            }
        });
    }

    private void setupCityDistrictWardSpinners() {
        List<String> cities = LocationHelper.getCities();
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cities);
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spCity != null) spCity.setAdapter(cityAdapter);

        if (spCity != null) {
            spCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (isRestoringLocation) return;
                    String selectedCity = cities.get(position);
                    updateDistrictsForCity(selectedCity, null, null);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }

    private void restoreLocationSelection(String targetCity, String targetDistrict, String targetWard) {
        List<String> cities = LocationHelper.getCities();
        if (spCity != null && targetCity != null) {
            for (int i = 0; i < cities.size(); i++) {
                if (cities.get(i).equalsIgnoreCase(targetCity)
                        || cities.get(i).toLowerCase().contains(targetCity.toLowerCase())
                        || targetCity.toLowerCase().contains(cities.get(i).toLowerCase())) {
                    try {
                        isRestoringLocation = true;
                        spCity.setSelection(i);
                        updateDistrictsForCity(cities.get(i), targetDistrict, targetWard);
                    } finally {
                        isRestoringLocation = false;
                    }
                    break;
                }
            }
        }
    }

    private void updateDistrictsForCity(String city, String targetDistrict) {
        updateDistrictsForCity(city, targetDistrict, null);
    }

    private void updateDistrictsForCity(String city, String targetDistrict, String targetWard) {
        List<String> districts = LocationHelper.getDistrictsForCity(city);
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, districts);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spDistrict != null) {
            spDistrict.setAdapter(districtAdapter);
            final List<String> currentDistricts = districts;
            spDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (isRestoringLocation) return;
                    if (position < currentDistricts.size()) {
                        String selectedDistrict = currentDistricts.get(position);
                        updateWardsForDistrict(selectedDistrict, null);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            if (targetDistrict != null) {
                for (int i = 0; i < districts.size(); i++) {
                    if (districts.get(i).equalsIgnoreCase(targetDistrict)
                            || districts.get(i).toLowerCase().contains(targetDistrict.toLowerCase())
                            || targetDistrict.toLowerCase().contains(districts.get(i).toLowerCase())) {
                        spDistrict.setSelection(i);
                        updateWardsForDistrict(districts.get(i), targetWard);
                        break;
                    }
                }
            }
        }
    }

    private void updateWardsForDistrict(String district) {
        updateWardsForDistrict(district, null);
    }

    private void updateWardsForDistrict(String district, String targetWard) {
        List<String> wards = LocationHelper.getWardsForDistrict(district);
        ArrayAdapter<String> wardAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, wards);
        wardAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spWard != null) {
            spWard.setAdapter(wardAdapter);
            if (targetWard != null) {
                for (int i = 0; i < wards.size(); i++) {
                    if (wards.get(i).equalsIgnoreCase(targetWard)
                            || wards.get(i).toLowerCase().contains(targetWard.toLowerCase())
                            || targetWard.toLowerCase().contains(wards.get(i).toLowerCase())) {
                        spWard.setSelection(i);
                        break;
                    }
                }
            }
        }
    }

    private void setupLocationPermissionLauncher() {
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    if ((fineGranted != null && fineGranted) || (coarseGranted != null && coarseGranted)) {
                        fetchCurrentLocation();
                    } else {
                        Toast.makeText(this, "Bạn đã từ chối quyền vị trí. Bạn có thể chọn thủ công.", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchCurrentLocation() {
        try {
            Toast.makeText(this, "Đang định vị trí chính xác...", Toast.LENGTH_SHORT).show();
            CurrentLocationRequest request = new CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setDurationMillis(10000)
                    .build();

            fusedLocationClient.getCurrentLocation(request, null).addOnSuccessListener(this, location -> {
                if (location != null) {
                    double safeLat = LocationHelper.normalizeLatitude(location.getLatitude(), location.getLongitude());
                    double safeLng = LocationHelper.normalizeLongitude(location.getLatitude(), location.getLongitude());
                    Geocoder geocoder = new Geocoder(this, new Locale("vi", "VN"));
                    try {
                        List<Address> addresses = geocoder.getFromLocation(safeLat, safeLng, 1);
                        if (addresses != null && !addresses.isEmpty()) {
                            Address address = addresses.get(0);
                            String city = address.getAdminArea();
                            String district = address.getSubAdminArea() != null ? address.getSubAdminArea() : address.getLocality();
                            String ward = address.getSubLocality() != null ? address.getSubLocality() : address.getThoroughfare();

                            if (city != null && spCity != null) {
                                ArrayAdapter<String> cityAdapter = (ArrayAdapter<String>) spCity.getAdapter();
                                if (cityAdapter != null) {
                                    for (int i = 0; i < cityAdapter.getCount(); i++) {
                                        String item = cityAdapter.getItem(i);
                                        if (item != null && (city.toLowerCase().contains(item.toLowerCase()) || item.toLowerCase().contains(city.toLowerCase()))) {
                                            spCity.setSelection(i);
                                            updateDistrictsForCity(item, district);
                                            break;
                                        }
                                    }
                                }
                            }

                            if (ward != null && spWard != null) {
                                spWard.postDelayed(() -> {
                                    ArrayAdapter<String> wardAdapter = (ArrayAdapter<String>) spWard.getAdapter();
                                    if (wardAdapter != null) {
                                        for (int i = 0; i < wardAdapter.getCount(); i++) {
                                            String item = wardAdapter.getItem(i);
                                            if (item != null && (ward.toLowerCase().contains(item.toLowerCase()) || item.toLowerCase().contains(ward.toLowerCase()))) {
                                                spWard.setSelection(i);
                                                break;
                                            }
                                        }
                                    }
                                }, 300);
                            }

                            Toast.makeText(this, "Đã lấy vị trí hiện tại thành công!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Không thể giải mã tọa độ vị trí.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        Toast.makeText(this, "Lỗi Geocoder: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Không thể lấy vị trí hiện tại. Hãy bật GPS trên thiết bị.", Toast.LENGTH_LONG).show();
                }
            });
        } catch (SecurityException e) {
            Toast.makeText(this, "Lỗi quyền vị trí: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        btnCancel.setOnClickListener(v -> finish());

        cardAddImage.setOnClickListener(v -> pickImageLauncher.launch(new String[]{"image/*"}));

        rgCondition.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbConditionOther) {
                    etConditionOther.setVisibility(View.VISIBLE);
                    etConditionOther.requestFocus();
                } else {
                    etConditionOther.setVisibility(View.GONE);
                    etConditionOther.setText("");
                }
            }
        });

        rgSharingType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbShareFree) {
                layoutExchangeContainer.setVisibility(View.GONE);
                layoutFeeLocationContainer.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbShareExchange) {
                layoutExchangeContainer.setVisibility(View.VISIBLE);
                layoutFeeLocationContainer.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbShareFee) {
                layoutExchangeContainer.setVisibility(View.GONE);
                layoutFeeLocationContainer.setVisibility(View.VISIBLE);
            }
        });

        if (ivMapLocation != null) {
            ivMapLocation.setOnClickListener(v -> checkAndRequestLocationPermission());
        }

        btnSubmit.setOnClickListener(v -> submitPost());
    }

    private void submitPost() {
        String title = etTitle.getText().toString().trim();
        String author = etAuthor.getText().toString().trim();
        // Lọc sạch mọi metadata cũ nếu người dùng dán hoặc sửa từ nội dung cũ
        String description = stripAppendedMetadata(etDescription.getText().toString());

        if (title.isEmpty() || author.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên sách và tác giả", Toast.LENGTH_SHORT).show();
            return;
        }

        String category = spinnerCategory.getSelectedItem().toString();

        String condition = "Mới 99%";
        int checkedCondId = rgCondition.getCheckedRadioButtonId();
        if (checkedCondId == R.id.rbConditionGood) {
            condition = "Tốt";
        } else if (checkedCondId == R.id.rbConditionOld) {
            condition = "Cũ vừa";
        } else if (checkedCondId == R.id.rbConditionOther) {
            condition = etConditionOther.getText().toString().trim();
            if (condition.isEmpty()) {
                etConditionOther.setError("Vui lòng nhập tình trạng sách");
                etConditionOther.requestFocus();
                return;
            }
        }

        String sharingType = "Tặng miễn phí";
        String bookStatus = "available";
        final String finalDesc;
        int checkedShareId = rgSharingType.getCheckedRadioButtonId();

        String descPrefix = description.isEmpty() ? "" : description + "\n\n";

        if (checkedShareId == R.id.rbShareExchange) {
            String exTitle = etExchangeBookTitle != null ? etExchangeBookTitle.getText().toString().trim() : "";
            String exAuthor = etExchangeBookAuthor != null ? etExchangeBookAuthor.getText().toString().trim() : "";
            if (exTitle.isEmpty()) {
                exTitle = "Sách cùng thể loại";
            }
            if (exAuthor.isEmpty()) {
                exAuthor = "Tùy chọn";
            }
            sharingType = "Trao đổi sách khác";
            bookStatus = "exchanging";
            finalDesc = descPrefix + "Hình thức: " + sharingType + "\n- Sách muốn đổi: " + exTitle + " (Tác giả: " + exAuthor + ")";
        } else if (checkedShareId == R.id.rbShareFee) {
            String priceStr = etPrice.getText().toString().trim();
            if (priceStr.isEmpty()) {
                etPrice.setError("Vui lòng nhập giá mong muốn");
                etPrice.requestFocus();
                return;
            }
            String city = spCity != null && spCity.getSelectedItem() != null ? spCity.getSelectedItem().toString() : "";
            String district = spDistrict != null && spDistrict.getSelectedItem() != null ? spDistrict.getSelectedItem().toString() : "";
            String ward = spWard != null && spWard.getSelectedItem() != null ? spWard.getSelectedItem().toString() : "";
            sharingType = "Phí tượng trưng / Giá rẻ";
            bookStatus = "available";
            finalDesc = descPrefix + "Hình thức: " + sharingType + "\n- Giá: " + priceStr + " VNĐ\n- Vị trí: " + ward + ", " + district + ", " + city;
        } else {
            finalDesc = descPrefix + "Hình thức: " + sharingType;
        }

        String finalCondition = condition;
        // TC-ADMIN-06: Bài đăng mới của Member phải chờ Admin kiểm duyệt trước khi lên Trang chủ.
        // Khi chỉnh sửa: giữ nguyên trạng thái "Chờ duyệt"/"Đã ẩn" để Member không tự mở lại bài.
        String finalStatus;
        if (editBookId == null || editBookId.trim().isEmpty()) {
            finalStatus = Book.STATUS_PENDING_REVIEW;
        } else if (existingBook != null && (Book.isPendingReview(existingBook)
                || Book.STATUS_HIDDEN.equalsIgnoreCase(existingBook.getStatus()))) {
            finalStatus = existingBook.getStatus();
        } else {
            finalStatus = bookStatus;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText(editBookId != null ? "Đang lưu..." : "Đang đăng...");

        if (selectedImageUri != null) {
            bookViewModel.uploadBookImage(selectedImageUri, new BookRepository.UploadImageCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    saveBookToDb(title, author, category, finalCondition, finalDesc, imageUrl, finalStatus);
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(CreatePostActivity.this, "Lỗi tải ảnh: " + errorMessage, Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText(editBookId != null ? "Lưu cập nhật" : "Tiếp tục & Xem trước");
                }
            });
        } else {
            String existingImg = (existingBook != null && existingBook.getImageUrl() != null) ? existingBook.getImageUrl() : "";
            saveBookToDb(title, author, category, finalCondition, finalDesc, existingImg, finalStatus);
        }
    }

    private void saveBookToDb(String title, String author, String category, String condition, String description, String imageUrl, String status) {
        String ownerId = authRepository.getCurrentUserId();
        if (ownerId == null) ownerId = "anonymous";

        String idToSave = (editBookId != null && !editBookId.trim().isEmpty()) ? editBookId : "";
        long createdAtToSave = (existingBook != null && existingBook.getCreatedAt() > 0) ? existingBook.getCreatedAt() : System.currentTimeMillis();

        Book book = new Book(idToSave, title, author, description, imageUrl, ownerId, createdAtToSave, category, condition, status);
        if (existingBook != null) {
            book.setViewCount(existingBook.getViewCount());
            book.setFavoriteCount(existingBook.getFavoriteCount());
        }

        bookViewModel.saveBook(book, new BookRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(CreatePostActivity.this,
                        editBookId != null ? "Cập nhật thông tin sách thành công!" : "Đã gửi bài đăng! Sách đang chờ Admin kiểm duyệt trước khi hiển thị trên Trang chủ.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(CreatePostActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
                btnSubmit.setEnabled(true);
                btnSubmit.setText(editBookId != null ? "Lưu cập nhật" : "Tiếp tục & Xem trước");
            }
        });
    }

    private static class BookSuggestionAdapter extends ArrayAdapter<Book> {
        private final List<Book> originalList;
        private final List<Book> filteredList;

        public BookSuggestionAdapter(@NonNull Context context, @NonNull List<Book> books) {
            super(context, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(books));
            this.originalList = new ArrayList<>(books);
            this.filteredList = new ArrayList<>(books);
        }

        @Override
        public int getCount() {
            return filteredList.size();
        }

        @Nullable
        @Override
        public Book getItem(int position) {
            if (position >= 0 && position < filteredList.size()) {
                return filteredList.get(position);
            }
            return null;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
            }
            TextView tv = convertView.findViewById(android.R.id.text1);
            Book b = getItem(position);
            if (b != null) {
                String title = b.getTitle() != null ? b.getTitle() : "";
                String author = b.getAuthor() != null ? b.getAuthor() : "Chưa rõ tác giả";
                tv.setText(title + " - " + author);
                tv.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            }
            return convertView;
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    List<Book> suggestions = new ArrayList<>();

                    if (constraint != null && constraint.length() > 0) {
                        String query = constraint.toString().toLowerCase().trim();
                        for (Book b : originalList) {
                            String title = b.getTitle() != null ? b.getTitle().toLowerCase() : "";
                            String author = b.getAuthor() != null ? b.getAuthor().toLowerCase() : "";
                            String cat = b.getCategory() != null ? b.getCategory().toLowerCase() : "";

                            if (title.contains(query) || author.contains(query) || cat.contains(query)) {
                                suggestions.add(b);
                            }
                        }
                    }

                    // Sắp xếp gợi ý theo Tên sách (A-Z) tương tự như ở Dashboard
                    suggestions.sort((b1, b2) -> {
                        String t1 = b1.getTitle() != null ? b1.getTitle() : "";
                        String t2 = b2.getTitle() != null ? b2.getTitle() : "";
                        return t1.compareToIgnoreCase(t2);
                    });

                    results.values = suggestions;
                    results.count = suggestions.size();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    filteredList.clear();
                    if (results != null && results.count > 0) {
                        filteredList.addAll((List<Book>) results.values);
                    }
                    notifyDataSetChanged();
                }

                @Override
                public CharSequence convertResultToString(Object resultValue) {
                    if (resultValue instanceof Book) {
                        return ((Book) resultValue).getTitle();
                    }
                    return super.convertResultToString(resultValue);
                }
            };
        }
    }
}
