package vn.edu.ueh.rtmiue20.project.ui.profile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.User;
import vn.edu.ueh.rtmiue20.project.data.repository.AuthRepository;
import vn.edu.ueh.rtmiue20.project.data.repository.UserRepository;
import vn.edu.ueh.rtmiue20.project.ui.admin.AdminDashboardActivity;
import vn.edu.ueh.rtmiue20.project.ui.home.HomeActivity;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView btnBackEditProfile;
    private View btnSaveProfile;
    private View btnSaveProfileMain;
    private View btnCancelEditProfile;
    private EditText etEditFullName;
    private EditText etEditNickname;
    private EditText etEditPhone;
    private EditText etEditBio;
    private TextView tvBioCounter;
    private TextView tvEditEmail;
    private TextView tvEditHeaderName;
    private TextView tvEditHeaderEmail;

    private Spinner spCity;
    private Spinner spDistrict;
    private Spinner spWard;
    private ImageView ivMapLocation;

    private View cardFavoriteSpot;
    private TextView tvFavoriteSpotName;
    private TextView tvFavoriteSpotAddress;

    private String selectedFavoriteSpotName = "";
    private String selectedFavoriteSpotAddress = "";

    private final Map<String, List<String>> cityToDistricts = new HashMap<>();
    private final Map<String, List<String>> districtToWards = new HashMap<>();

    private AuthRepository authRepository;
    private UserRepository userRepository;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainEditProfile), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        authRepository = AuthRepository.getInstance();
        userRepository = UserRepository.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupSpinners();
        setupLocationPermissionLauncher();
        setupBioCounter();
        loadExistingData();
        setupListeners();
    }

    private void initViews() {
        btnBackEditProfile = findViewById(R.id.btnBackEditProfile);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnSaveProfileMain = findViewById(R.id.btnSaveProfileMain);
        btnCancelEditProfile = findViewById(R.id.btnCancelEditProfile);
        etEditFullName = findViewById(R.id.etEditFullName);
        etEditNickname = findViewById(R.id.etEditNickname);
        etEditPhone = findViewById(R.id.etEditPhone);
        etEditBio = findViewById(R.id.etEditBio);
        tvBioCounter = findViewById(R.id.tvBioCounter);
        tvEditEmail = findViewById(R.id.tvEditEmail);
        tvEditHeaderName = findViewById(R.id.tvEditHeaderName);
        tvEditHeaderEmail = findViewById(R.id.tvEditHeaderEmail);

        spCity = findViewById(R.id.spCity);
        spDistrict = findViewById(R.id.spDistrict);
        spWard = findViewById(R.id.spWard);
        ivMapLocation = findViewById(R.id.ivMapLocation);

        cardFavoriteSpot = findViewById(R.id.cardFavoriteSpot);
        tvFavoriteSpotName = findViewById(R.id.tvFavoriteSpotName);
        tvFavoriteSpotAddress = findViewById(R.id.tvFavoriteSpotAddress);
    }

    private void setupSpinners() {
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
                        updateWardsForDistrict(selectedDistrict, null);
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

    private void updateWardsForDistrict(String district, String targetWard) {
        List<String> wards = districtToWards.get(district);
        if (wards == null) {
            wards = Arrays.asList("Phường 1", "Phường 2", "Phường 3", "Phường 4", "Phường 5");
        }
        ArrayAdapter<String> wardAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, wards);
        wardAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spWard != null) {
            spWard.setAdapter(wardAdapter);
            if (targetWard != null) {
                for (int i = 0; i < wards.size(); i++) {
                    if (wards.get(i).toLowerCase().contains(targetWard.toLowerCase()) || targetWard.toLowerCase().contains(wards.get(i).toLowerCase())) {
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
                        Toast.makeText(this, "Bạn đã từ chối quyền vị trí. Bạn có thể nhập thông tin vị trí thủ công.", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void setupBioCounter() {
        if (etEditBio != null && tvBioCounter != null) {
            etEditBio.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    int len = s != null ? s.length() : 0;
                    tvBioCounter.setText(len + "/200");
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void loadExistingData() {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        if (tvEditEmail != null && currentUser.getEmail() != null) {
            tvEditEmail.setText(currentUser.getEmail());
        }
        if (tvEditHeaderEmail != null && currentUser.getEmail() != null) {
            tvEditHeaderEmail.setText(currentUser.getEmail());
        }

        String initialName = currentUser.getDisplayName();
        if (initialName == null || initialName.trim().isEmpty()) {
            android.content.SharedPreferences prefs = getSharedPreferences("BookSharePrefs", MODE_PRIVATE);
            initialName = prefs.getString("user_fullname", "");
        }
        if (etEditFullName != null && initialName != null && !initialName.trim().isEmpty()) {
            etEditFullName.setText(initialName);
        }

        userRepository.getUserProfile(currentUser.getUid(), new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (etEditFullName != null && user.getFullName() != null) etEditFullName.setText(user.getFullName());
                if (etEditNickname != null && user.getNickname() != null) etEditNickname.setText(user.getNickname());
                if (etEditPhone != null && user.getPhoneNumber() != null) etEditPhone.setText(user.getPhoneNumber());
                if (etEditBio != null && user.getBio() != null) etEditBio.setText(user.getBio());

                if (user.getFavoriteSpotName() != null && !user.getFavoriteSpotName().isEmpty()) {
                    selectedFavoriteSpotName = user.getFavoriteSpotName();
                    if (tvFavoriteSpotName != null) tvFavoriteSpotName.setText(selectedFavoriteSpotName);
                } else {
                    if (tvFavoriteSpotName != null) tvFavoriteSpotName.setText("Chưa chọn điểm hẹn");
                    if (tvFavoriteSpotAddress != null) tvFavoriteSpotAddress.setText("Chạm để thêm điểm hẹn giao dịch");
                }
                if (user.getFavoriteSpotAddress() != null && !user.getFavoriteSpotAddress().isEmpty()) {
                    selectedFavoriteSpotAddress = user.getFavoriteSpotAddress();
                    if (tvFavoriteSpotAddress != null) tvFavoriteSpotAddress.setText(selectedFavoriteSpotAddress);
                }

                String formattedName = User.getDisplayNameFormatted(user.getFullName(), user.getNickname());
                if (tvEditHeaderName != null) {
                    tvEditHeaderName.setText(!formattedName.isEmpty() ? formattedName : "Người dùng BookShare");
                }

                if (user.getCity() != null && spCity != null) {
                    ArrayAdapter adapter = (ArrayAdapter) spCity.getAdapter();
                    if (adapter != null) {
                        int pos = adapter.getPosition(user.getCity());
                        if (pos >= 0) {
                            spCity.setSelection(pos);
                            updateDistrictsForCity(user.getCity(), user.getDistrict());
                        }
                    }
                }
                if (user.getWard() != null && spWard != null) {
                    spWard.postDelayed(() -> {
                        ArrayAdapter wardAdapter = (ArrayAdapter) spWard.getAdapter();
                        if (wardAdapter != null) {
                            int pos = wardAdapter.getPosition(user.getWard());
                            if (pos >= 0) spWard.setSelection(pos);
                        }
                    }, 300);
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (etEditFullName != null && currentUser.getDisplayName() != null) {
                    etEditFullName.setText(currentUser.getDisplayName());
                }
                if (tvEditHeaderName != null && currentUser.getDisplayName() != null) {
                    tvEditHeaderName.setText(currentUser.getDisplayName());
                }
            }
        });
    }

    private void setupListeners() {
        if (btnBackEditProfile != null) btnBackEditProfile.setOnClickListener(v -> finishOrNavigate());
        if (btnCancelEditProfile != null) btnCancelEditProfile.setOnClickListener(v -> finishOrNavigate());

        View.OnClickListener saveListener = v -> handleSaveProfile();
        if (btnSaveProfile != null) btnSaveProfile.setOnClickListener(saveListener);
        if (btnSaveProfileMain != null) btnSaveProfileMain.setOnClickListener(saveListener);

        if (ivMapLocation != null) {
            ivMapLocation.setOnClickListener(v -> checkAndRequestLocationPermission());
        }

        if (cardFavoriteSpot != null) {
            cardFavoriteSpot.setOnClickListener(v -> showFavoriteSpotsDialog());
        }
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

                            Toast.makeText(this, "Đã lấy vị trí chính xác thành công!", Toast.LENGTH_SHORT).show();
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

    private void showFavoriteSpotsDialog() {
        SharedPreferences prefs = getSharedPreferences("BookSharePrefs", MODE_PRIVATE);
        String historySpotName = prefs.getString("last_checkin_spot_name", "");
        String historySpotAddress = prefs.getString("last_checkin_spot_address", "");

        List<String> spotNames = new ArrayList<>();
        List<String> spotAddresses = new ArrayList<>();

        if (selectedFavoriteSpotName != null && !selectedFavoriteSpotName.isEmpty() && !selectedFavoriteSpotName.equals("Chưa chọn điểm hẹn") && !selectedFavoriteSpotName.equals("Chạm để thêm điểm hẹn giao dịch")) {
            spotNames.add(selectedFavoriteSpotName);
            spotAddresses.add(selectedFavoriteSpotAddress);
        }
        if (!historySpotName.isEmpty() && !spotNames.contains(historySpotName)) {
            spotNames.add(historySpotName);
            spotAddresses.add(historySpotAddress);
        }

        if (spotNames.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Điểm hẹn giao nhận ưa thích")
                    .setMessage("Chưa có lịch sử check-in điểm hẹn nào. Điểm hẹn sẽ được ghi nhận và lưu lại tự động khi bạn thực hiện các giao dịch trao đổi sách hoặc check-in vị trí trong ứng dụng.")
                    .setPositiveButton("Đã hiểu", null)
                    .show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Chọn điểm hẹn giao nhận (Dựa trên lịch sử check-in)");
            builder.setItems(spotNames.toArray(new String[0]), (dialog, which) -> {
                selectedFavoriteSpotName = spotNames.get(which);
                selectedFavoriteSpotAddress = spotAddresses.get(which);
                if (tvFavoriteSpotName != null) tvFavoriteSpotName.setText(selectedFavoriteSpotName);
                if (tvFavoriteSpotAddress != null) tvFavoriteSpotAddress.setText(selectedFavoriteSpotAddress);
                Toast.makeText(this, "Đã chọn: " + selectedFavoriteSpotName, Toast.LENGTH_SHORT).show();
            });
            builder.show();
        }
    }

    private boolean isFromLogin() {
        return getIntent().getBooleanExtra("from_login", false);
    }

    private void finishOrNavigate() {
        if (isFromLogin()) {
            navigateAfterSaveProfile();
        } else {
            finish();
        }
    }

    private void navigateAfterSaveProfile() {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser != null) {
            userRepository.getUserProfile(currentUser.getUid(), new UserRepository.UserCallback() {
                @Override
                public void onSuccess(User userProfile) {
                    Intent intent;
                    if (userProfile != null && "admin".equalsIgnoreCase(userProfile.getRole())) {
                        intent = new Intent(EditProfileActivity.this, AdminDashboardActivity.class);
                    } else {
                        intent = new Intent(EditProfileActivity.this, HomeActivity.class);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    Intent intent = new Intent(EditProfileActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        } else {
            Intent intent = new Intent(EditProfileActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void handleSaveProfile() {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) return;

        String fullName = etEditFullName != null ? etEditFullName.getText().toString().trim() : "";
        String nickname = etEditNickname != null ? etEditNickname.getText().toString().trim() : "";
        String phone = etEditPhone != null ? etEditPhone.getText().toString().trim() : "";
        String bio = etEditBio != null ? etEditBio.getText().toString().trim() : "";
        String city = spCity != null && spCity.getSelectedItem() != null ? spCity.getSelectedItem().toString() : "";
        String district = spDistrict != null && spDistrict.getSelectedItem() != null ? spDistrict.getSelectedItem().toString() : "";
        String ward = spWard != null && spWard.getSelectedItem() != null ? spWard.getSelectedItem().toString() : "";

        if (fullName.isEmpty()) {
            if (etEditFullName != null) {
                etEditFullName.setError("Vui lòng nhập họ và tên");
                etEditFullName.requestFocus();
            }
            return;
        }

        if (btnSaveProfile != null) btnSaveProfile.setEnabled(false);
        if (btnSaveProfileMain != null) btnSaveProfileMain.setEnabled(false);

        userRepository.updateUserProfile(currentUser.getUid(), fullName, nickname, phone, bio, city, district, ward, selectedFavoriteSpotName, selectedFavoriteSpotAddress, new UserRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                String displayNameFormatted = User.getDisplayNameFormatted(fullName, nickname);
                android.content.SharedPreferences prefs = getSharedPreferences("BookSharePrefs", MODE_PRIVATE);
                prefs.edit()
                        .putString("user_fullname", !displayNameFormatted.isEmpty() ? displayNameFormatted : fullName)
                        .putString("user_nickname", nickname)
                        .apply();

                Toast.makeText(EditProfileActivity.this, "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                finishOrNavigate();
            }

            @Override
            public void onError(String errorMessage) {
                if (btnSaveProfile != null) btnSaveProfile.setEnabled(true);
                if (btnSaveProfileMain != null) btnSaveProfileMain.setEnabled(true);
                Toast.makeText(EditProfileActivity.this, "Lỗi cập nhật: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
