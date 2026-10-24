package vn.edu.ueh.rtmiue20.project.ui.book;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.Book;
import vn.edu.ueh.rtmiue20.project.data.repository.AuthRepository;
import vn.edu.ueh.rtmiue20.project.data.repository.BookRepository;
import vn.edu.ueh.rtmiue20.project.ui.home.BookViewModel;

public class CreatePostActivity extends AppCompatActivity {

    private ImageView btnCancel, ivPreviewImage, ivMapLocation;
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

    private final Map<String, List<String>> cityToDistricts = new HashMap<>();
    private final Map<String, List<String>> districtToWards = new HashMap<>();

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

        initViews();
        setupSpinner();
        setupCityDistrictWardSpinners();
        setupLocationPermissionLauncher();
        setupExchangeBookAutocomplete();
        setupListeners();
    }

    private void initViews() {
        btnCancel = findViewById(R.id.btnCancel);
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

    private void setupSpinner() {
        String[] categories = {"Giáo trình", "Văn học", "Kỹ năng", "Kinh tế", "Khác"};
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
        cityToDistricts.put("Hồ Chí Minh", Arrays.asList("Quận 1", "Quận 3", "Quận 5", "Quận 8", "Quận 10", "Bình Thạnh", "Tân Bình", "TP. Thủ Đức"));
        cityToDistricts.put("Hà Nội", Arrays.asList("Quận Cầu Giấy", "Quận Đống Đa", "Quận Hoàn Kiếm", "Quận Ba Đình"));
        cityToDistricts.put("Đà Nẵng", Arrays.asList("Quận Hải Châu", "Quận Thanh Khê", "Quận Sơn Trà"));
        cityToDistricts.put("Cần Thơ", Arrays.asList("Quận Ninh Kiều", "Quận Bình Thủy", "Quận Cái Răng"));
        cityToDistricts.put("Hải Phòng", Arrays.asList("Quận Hồng Bàng", "Quận Ngô Quyền", "Quận Lê Chân"));
        cityToDistricts.put("Bình Dương", Arrays.asList("Thành phố Thủ Dầu Một", "Thành phố Dĩ An", "Thành phố Thuận An"));
        cityToDistricts.put("Đồng Nai", Arrays.asList("Thành phố Biên Hòa", "Huyện Long Thành", "Huyện Nhơn Trạch"));

        districtToWards.put("Quận 1", Arrays.asList("Phường Bến Nghé", "Phường Bến Thành", "Phường Cô Giang", "Phường Cầu Kho", "Phường Cầu Ông Lãnh", "Phường Đa Kao", "Phường Nguyễn Cư Trinh", "Phường Nguyễn Thái Bình", "Phường Phạm Ngũ Lão", "Phường Tân Định"));
        districtToWards.put("Quận 3", Arrays.asList("Phường Võ Thị Sáu", "Phường 1", "Phường 2", "Phường 3", "Phường 4", "Phường 5", "Phường 9", "Phường 10", "Phường 11", "Phường 12", "Phường 13", "Phường 14"));
        districtToWards.put("Quận 5", Arrays.asList("Phường 1", "Phường 2", "Phường 3", "Phường 4", "Phường 5", "Phường 6", "Phường 7", "Phường 8", "Phường 9", "Phường 10", "Phường 11", "Phường 12", "Phường 13", "Phường 14"));
        districtToWards.put("Quận 8", Arrays.asList("Phường 1", "Phường 2", "Phường 3", "Phường 4", "Phường 5", "Phường 6", "Phường 7", "Phường 8", "Phường 9", "Phường 10", "Phường 11", "Phường 12", "Phường 13", "Phường 14", "Phường 15", "Phường 16"));
        districtToWards.put("Quận 10", Arrays.asList("Phường 1", "Phường 2", "Phường 3", "Phường 4", "Phường 5", "Phường 6", "Phường 7", "Phường 8", "Phường 9", "Phường 10", "Phường 11", "Phường 12", "Phường 13", "Phường 14", "Phường 15"));
        districtToWards.put("Bình Thạnh", Arrays.asList("Phường 1", "Phường 2", "Phường 3", "Phường 5", "Phường 6", "Phường 7", "Phường 11", "Phường 12", "Phường 13", "Phường 14", "Phường 15", "Phường 17", "Phường 19", "Phường 21", "Phường 22", "Phường 24", "Phường 25", "Phường 26", "Phường 27", "Phường 28"));
        districtToWards.put("Tân Bình", Arrays.asList("Phường 1", "Phường 2", "Phường 3", "Phường 4", "Phường 5", "Phường 6", "Phường 7", "Phường 8", "Phường 9", "Phường 10", "Phường 11", "Phường 12", "Phường 13", "Phường 14", "Phường 15"));
        districtToWards.put("TP. Thủ Đức", Arrays.asList("Phường An Khánh", "Phường An Lợi Đông", "Phường An Phú", "Phường Bình Chiểu", "Phường Bình Thọ", "Phường Bình Trưng Đông", "Phường Bình Trưng Tây", "Phường Cát Lái", "Phường Hiệp Bình Chánh", "Phường Hiệp Bình Phước", "Phường Hiệp Phú", "Phường Linh Chiểu", "Phường Linh Đông", "Phường Linh Tây", "Phường Linh Trung", "Phường Linh Xuân", "Phường Long Bình", "Phường Long Phước", "Phường Long Thạnh Mỹ", "Phường Long Trường", "Phường Phú Hữu", "Phường Phước Long A", "Phường Phước Long B", "Phường Phước Thạnh", "Phường Tam Bình", "Phường Tam Phú", "Phường Tăng Nhơn Phú A", "Phường Tăng Nhơn Phú B", "Phường Tân Phú", "Phường Thạnh Mỹ Lợi", "Phường Thảo Điền", "Phường Thủ Thiêm", "Phường Trường Thạnh", "Phường Trường Thọ"));
        districtToWards.put("Quận Cầu Giấy", Arrays.asList("Phường Dịch Vọng", "Phường Dịch Vọng Hậu", "Phường Mai Dịch", "Phường Nghĩa Đô", "Phường Nghĩa Tân", "Phường Quan Hoa", "Phường Trung Hòa", "Phường Yên Hòa"));
        districtToWards.put("Quận Đống Đa", Arrays.asList("Phường Cát Linh", "Phường Hàng Bột", "Phường Khâm Thiên", "Phường Khương Thượng", "Phường Kim Liên", "Phường Láng Hạ", "Phường Láng Thượng", "Phường Nam Đồng", "Phường Ngã Tư Sở", "Phường Ô Chợ Dừa", "Phường Phương Liên", "Phường Phương Mai", "Phường Quang Trung", "Phường Quốc Tử Giám", "Phường Thịnh Quang", "Phường Thổ Quan", "Phường Trung Liệt", "Phường Trung Phụng", "Phường Văn Chương", "Phường Văn Miếu"));
        districtToWards.put("Quận Hoàn Kiếm", Arrays.asList("Phường Chương Dương", "Phường Cửa Đông", "Phường Cửa Nam", "Phường Đồng Xuân", "Phường Hàng Bạc", "Phường Hàng Bài", "Phường Hàng Bồ", "Phường Hàng Bông", "Phường Hàng Buồm", "Phường Hàng Đào", "Phường Hàng Gai", "Phường Hàng Mã", "Phường Hàng Trống", "Phường Lý Thái Tổ", "Phường Phan Chu Trinh", "Phường Phúc Tân", "Phường Tràng Tiền", "Phường Trần Hưng Đạo"));
        districtToWards.put("Quận Ba Đình", Arrays.asList("Phường Cống Vị", "Phường Điện Biên", "Phường Đội Cấn", "Phường Giảng Võ", "Phường Kim Mã", "Phường Liễu Giai", "Phường Ngọc Hà", "Phường Ngọc Khánh", "Phường Nguyễn Trung Trực", "Phường Phúc Xá", "Phường Quán Thánh", "Phường Thành Công", "Phường Trúc Bạch", "Phường Vĩnh Phúc"));

        List<String> cities = Arrays.asList("Hồ Chí Minh", "Hà Nội", "Đà Nẵng", "Cần Thơ", "Hải Phòng", "Bình Dương", "Đồng Nai");
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cities);
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spCity != null) spCity.setAdapter(cityAdapter);

        if (spCity != null) {
            spCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selectedCity = cities.get(position);
                    updateDistrictsForCity(selectedCity, null);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }

    private void updateDistrictsForCity(String city, String targetDistrict) {
        List<String> districts = cityToDistricts.get(city);
        if (districts == null) {
            districts = Arrays.asList("Quận trung tâm", "Huyện ngoại thành");
        }
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, districts);
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spDistrict != null) {
            spDistrict.setAdapter(districtAdapter);
            final List<String> currentDistricts = districts;
            spDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position < currentDistricts.size()) {
                        String selectedDistrict = currentDistricts.get(position);
                        updateWardsForDistrict(selectedDistrict);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            if (targetDistrict != null) {
                for (int i = 0; i < districts.size(); i++) {
                    if (districts.get(i).toLowerCase().contains(targetDistrict.toLowerCase()) || targetDistrict.toLowerCase().contains(districts.get(i).toLowerCase())) {
                        spDistrict.setSelection(i);
                        break;
                    }
                }
            }
        }
    }

    private void updateWardsForDistrict(String district) {
        List<String> wards = districtToWards.get(district);
        if (wards == null) {
            wards = Arrays.asList("Phường 1", "Phường 2", "Phường 3", "Phường 4", "Phường 5");
        }
        ArrayAdapter<String> wardAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, wards);
        wardAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spWard != null) {
            spWard.setAdapter(wardAdapter);
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
                    Geocoder geocoder = new Geocoder(this, new Locale("vi", "VN"));
                    try {
                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
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
        String description = etDescription.getText().toString().trim();

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
        final String finalDesc;
        int checkedShareId = rgSharingType.getCheckedRadioButtonId();

        if (checkedShareId == R.id.rbShareExchange) {
            String exTitle = etExchangeBookTitle.getText().toString().trim();
            String exAuthor = etExchangeBookAuthor.getText().toString().trim();
            if (exTitle.isEmpty() || exAuthor.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên sách và tác giả muốn đổi", Toast.LENGTH_SHORT).show();
                return;
            }
            sharingType = "Trao đổi sách khác";
            finalDesc = description + "\n\nHình thức: " + sharingType + "\n- Sách muốn đổi: " + exTitle + " (Tác giả: " + exAuthor + ")";
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
            finalDesc = description + "\n\nHình thức: " + sharingType + "\n- Giá: " + priceStr + " VNĐ\n- Vị trí: " + ward + ", " + district + ", " + city;
        } else {
            finalDesc = description + "\n\nHình thức: " + sharingType;
        }

        String finalCondition = condition;

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang đăng...");

        if (selectedImageUri != null) {
            bookViewModel.uploadBookImage(selectedImageUri, new BookRepository.UploadImageCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    saveBookToDb(title, author, category, finalCondition, finalDesc, imageUrl);
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(CreatePostActivity.this, "Lỗi tải ảnh: " + errorMessage, Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Tiếp tục & Xem trước");
                }
            });
        } else {
            saveBookToDb(title, author, category, finalCondition, finalDesc, "");
        }
    }

    private void saveBookToDb(String title, String author, String category, String condition, String description, String imageUrl) {
        String ownerId = authRepository.getCurrentUserId();
        if (ownerId == null) ownerId = "anonymous";

        Book book = new Book("", title, author, description, imageUrl, ownerId, System.currentTimeMillis(), category, condition, "available");

        bookViewModel.saveBook(book, new BookRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(CreatePostActivity.this, "Đăng sách thành công!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(CreatePostActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Tiếp tục & Xem trước");
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
