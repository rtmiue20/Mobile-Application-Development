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
import java.util.List;
import java.util.Locale;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.TypedValue;
import android.widget.FrameLayout;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.User;
import vn.edu.ueh.rtmiue20.project.data.repository.AuthRepository;
import vn.edu.ueh.rtmiue20.project.data.repository.UserRepository;
import vn.edu.ueh.rtmiue20.project.ui.admin.AdminDashboardActivity;
import vn.edu.ueh.rtmiue20.project.ui.home.HomeActivity;
import vn.edu.ueh.rtmiue20.project.util.LocationHelper;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView btnBackEditProfile;
    private ImageView ivEditAvatar;
    private View btnCameraAvatar;
    private View btnChangeAvatar;
    private View btnRemoveAvatar;
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

    private ChipGroup chipGroupInterests;
    private Chip chipAddInterest;
    private TextView tvSelectedInterestsCount;

    private SwitchMaterial swShowPhone;
    private SwitchMaterial swDirectRequest;
    private SwitchMaterial swBookAlerts;
    private TextView tvBookAlertsDesc;

    private static final String[] DEFAULT_CATEGORIES = {
        "Văn học Việt Nam",
        "Tiểu thuyết kinh điển",
        "Tâm lý - Kỹ năng",
        "Kinh tế - Khởi nghiệp",
        "Khoa học - Công nghệ",
        "Truyện tranh Manga",
        "Triết học & Tư tưởng",
        "Ngoại ngữ - Du học"
    };

    private String selectedFavoriteSpotName = "";
    private String selectedFavoriteSpotAddress = "";

    private AuthRepository authRepository;
    private UserRepository userRepository;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private ActivityResultLauncher<String[]> avatarPickerLauncher;
    private Double currentLatitude = null;
    private Double currentLongitude = null;

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
        setupAvatarPickerLauncher();
        setupBioCounter();
        loadExistingData();
        setupListeners();
    }

    private void initViews() {
        btnBackEditProfile = findViewById(R.id.btnBackEditProfile);
        ivEditAvatar = findViewById(R.id.ivEditAvatar);
        btnCameraAvatar = findViewById(R.id.btnCameraAvatar);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        btnRemoveAvatar = findViewById(R.id.btnRemoveAvatar);
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

        chipGroupInterests = findViewById(R.id.chipGroupInterests);
        chipAddInterest = findViewById(R.id.chipAddInterest);
        tvSelectedInterestsCount = findViewById(R.id.tvSelectedInterestsCount);
        setupInterests(null);

        swShowPhone = findViewById(R.id.swShowPhone);
        swDirectRequest = findViewById(R.id.swDirectRequest);
        swBookAlerts = findViewById(R.id.swBookAlerts);
        tvBookAlertsDesc = findViewById(R.id.tvBookAlertsDesc);

        SharedPreferences prefs = getSharedPreferences("BookSharePrefs", MODE_PRIVATE);
        if (swShowPhone != null) swShowPhone.setChecked(prefs.getBoolean("pref_show_phone", true));
        if (swDirectRequest != null) swDirectRequest.setChecked(prefs.getBoolean("pref_direct_request", true));
        if (swBookAlerts != null) swBookAlerts.setChecked(prefs.getBoolean("pref_book_alerts", true));
    }

    private void setupSpinners() {
        List<String> cities = LocationHelper.getCities();
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
        List<String> districts = LocationHelper.getDistrictsForCity(city);
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
        if (tvBookAlertsDesc != null) {
            if (district != null && !district.trim().isEmpty() && !district.equalsIgnoreCase("Chọn Quận/Huyện")) {
                tvBookAlertsDesc.setText("Cập nhật tự động khi có đầu sách mới tại " + district);
            } else {
                tvBookAlertsDesc.setText("Cập nhật tự động khi có đầu sách mới tại khu vực của bạn");
            }
        }
        List<String> wards = LocationHelper.getWardsForDistrict(district);
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

    private void setupAvatarPickerLauncher() {
        avatarPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        uploadNewAvatar(uri);
                    }
                }
        );
    }

    private void uploadNewAvatar(android.net.Uri imageUri) {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) return;

        Toast.makeText(this, "Đang cập nhật ảnh đại diện...", Toast.LENGTH_SHORT).show();
        userRepository.uploadAvatar(this, currentUser.getUid(), imageUri, new UserRepository.UploadCallback() {
            @Override
            public void onSuccess(String avatarUrl) {
                getSharedPreferences("BookSharePrefs", MODE_PRIVATE).edit()
                        .putString("user_avatar_url", avatarUrl != null ? avatarUrl : "")
                        .apply();
                vn.edu.ueh.rtmiue20.project.util.ImageHelper.bindAvatar(EditProfileActivity.this, ivEditAvatar, avatarUrl, false);
                Toast.makeText(EditProfileActivity.this, "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(EditProfileActivity.this, "Lỗi tải ảnh: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleRemoveAvatar() {
        FirebaseUser currentUser = authRepository.getCurrentUser();
        if (currentUser == null) return;

        userRepository.removeAvatar(currentUser.getUid(), new UserRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                getSharedPreferences("BookSharePrefs", MODE_PRIVATE).edit()
                        .putString("user_avatar_url", "")
                        .apply();
                vn.edu.ueh.rtmiue20.project.util.ImageHelper.bindAvatar(EditProfileActivity.this, ivEditAvatar, "", false);
                Toast.makeText(EditProfileActivity.this, "Đã xóa ảnh đại diện và khôi phục mặc định!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(EditProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
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

                String avatarUrl = user.getAvatarUrl() != null ? user.getAvatarUrl() : "";
                getSharedPreferences("BookSharePrefs", MODE_PRIVATE).edit()
                        .putString("user_avatar_url", avatarUrl)
                        .apply();
                vn.edu.ueh.rtmiue20.project.util.ImageHelper.bindAvatar(EditProfileActivity.this, ivEditAvatar, avatarUrl, false);

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

                if (user.getLatitude() != null && user.getLongitude() != null) {
                    currentLatitude = user.getLatitude();
                    currentLongitude = user.getLongitude();
                    getSharedPreferences("BookSharePrefs", MODE_PRIVATE).edit()
                            .putFloat("user_lat", currentLatitude.floatValue())
                            .putFloat("user_lng", currentLongitude.floatValue())
                            .apply();
                }

                setupInterests(user.getInterests());

                if (swShowPhone != null) swShowPhone.setChecked(user.isShowPhone());
                if (swDirectRequest != null) swDirectRequest.setChecked(user.isDirectRequest());
                if (swBookAlerts != null) swBookAlerts.setChecked(user.isBookAlerts());
                if (user.getDistrict() != null && !user.getDistrict().isEmpty() && tvBookAlertsDesc != null) {
                    tvBookAlertsDesc.setText("Cập nhật tự động khi có đầu sách mới tại " + user.getDistrict());
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
                setupInterests(null);
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

        View.OnClickListener pickAvatarListener = v -> {
            if (avatarPickerLauncher != null) {
                avatarPickerLauncher.launch(new String[]{"image/*"});
            }
        };
        if (ivEditAvatar != null) ivEditAvatar.setOnClickListener(pickAvatarListener);
        if (btnCameraAvatar != null) btnCameraAvatar.setOnClickListener(pickAvatarListener);
        if (btnChangeAvatar != null) btnChangeAvatar.setOnClickListener(pickAvatarListener);
        if (btnRemoveAvatar != null) {
            btnRemoveAvatar.setOnClickListener(v -> handleRemoveAvatar());
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

                            currentLatitude = safeLat;
                            currentLongitude = safeLng;
                            getSharedPreferences("BookSharePrefs", MODE_PRIVATE).edit()
                                    .putFloat("user_lat", (float) safeLat)
                                    .putFloat("user_lng", (float) safeLng)
                                    .apply();

                            Toast.makeText(this, "Đã lấy vị trí chính xác thành công!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Không thể giải mã tọa độ vị trí.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        currentLatitude = safeLat;
                        currentLongitude = safeLng;
                        getSharedPreferences("BookSharePrefs", MODE_PRIVATE).edit()
                                .putFloat("user_lat", (float) safeLat)
                                .putFloat("user_lng", (float) safeLng)
                                .apply();
                        Toast.makeText(this, "Đã lấy tọa độ GPS thành công!", Toast.LENGTH_SHORT).show();
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

        // Luôn cung cấp điểm hẹn mặc định của cộng đồng UEH Cơ sở B
        spotNames.add("UEH Cơ sở B (Điểm hẹn mặc định)");
        spotAddresses.add(LocationHelper.UEH_CAMPUS_B_ADDRESS);

        if (selectedFavoriteSpotName != null && !selectedFavoriteSpotName.isEmpty()
                && !selectedFavoriteSpotName.equals("Chưa chọn điểm hẹn")
                && !selectedFavoriteSpotName.equals("Chạm để thêm điểm hẹn giao dịch")
                && !spotNames.contains(selectedFavoriteSpotName)) {
            spotNames.add(selectedFavoriteSpotName);
            spotAddresses.add(selectedFavoriteSpotAddress);
        }
        if (!historySpotName.isEmpty() && !spotNames.contains(historySpotName)) {
            spotNames.add(historySpotName);
            spotAddresses.add(historySpotAddress);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn điểm hẹn giao nhận ưa thích");
        builder.setItems(spotNames.toArray(new String[0]), (dialog, which) -> {
            selectedFavoriteSpotName = spotNames.get(which);
            selectedFavoriteSpotAddress = spotAddresses.get(which);
            if (tvFavoriteSpotName != null) tvFavoriteSpotName.setText(selectedFavoriteSpotName);
            if (tvFavoriteSpotAddress != null) tvFavoriteSpotAddress.setText(selectedFavoriteSpotAddress);
            Toast.makeText(this, "Đã chọn: " + selectedFavoriteSpotName, Toast.LENGTH_SHORT).show();
        });
        builder.show();
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

        if (currentLatitude == null || currentLongitude == null) {
            String fullAddr = (ward.isEmpty() ? "" : ward + ", ") + (district.isEmpty() ? "" : district + ", ") + city;
            if (!fullAddr.trim().isEmpty()) {
                try {
                    Geocoder geocoder = new Geocoder(this, new Locale("vi", "VN"));
                    List<Address> list = geocoder.getFromLocationName(fullAddr, 1);
                    if (list != null && !list.isEmpty()) {
                        currentLatitude = list.get(0).getLatitude();
                        currentLongitude = list.get(0).getLongitude();
                    }
                } catch (Exception ignored) {}
            }
        }

        List<String> selectedInterests = getSelectedInterests();
        boolean isShowPhone = swShowPhone != null && swShowPhone.isChecked();
        boolean isDirectRequest = swDirectRequest != null && swDirectRequest.isChecked();
        boolean isBookAlerts = swBookAlerts != null && swBookAlerts.isChecked();

        userRepository.updateUserProfile(currentUser.getUid(), fullName, nickname, phone, bio, city, district, ward, selectedFavoriteSpotName, selectedFavoriteSpotAddress, currentLatitude, currentLongitude, selectedInterests, isShowPhone, isDirectRequest, isBookAlerts, new UserRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                String displayNameFormatted = User.getDisplayNameFormatted(fullName, nickname);
                android.content.SharedPreferences prefs = getSharedPreferences("BookSharePrefs", MODE_PRIVATE);
                android.content.SharedPreferences.Editor editor = prefs.edit()
                        .putString("user_fullname", !displayNameFormatted.isEmpty() ? displayNameFormatted : fullName)
                        .putString("user_nickname", nickname)
                        .putBoolean("pref_show_phone", isShowPhone)
                        .putBoolean("pref_direct_request", isDirectRequest)
                        .putBoolean("pref_book_alerts", isBookAlerts);
                if (currentLatitude != null && currentLongitude != null) {
                    editor.putFloat("user_lat", currentLatitude.floatValue());
                    editor.putFloat("user_lng", currentLongitude.floatValue());
                }
                editor.apply();

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

    private void setupInterests(List<String> savedInterests) {
        if (chipGroupInterests == null) return;

        // Xóa các chip cũ (giữ lại chip Thêm thể loại)
        for (int i = chipGroupInterests.getChildCount() - 1; i >= 0; i--) {
            View child = chipGroupInterests.getChildAt(i);
            if (child.getId() != R.id.chipAddInterest) {
                chipGroupInterests.removeViewAt(i);
            }
        }

        List<String> allCategories = new ArrayList<>();
        for (String cat : DEFAULT_CATEGORIES) {
            allCategories.add(cat);
        }

        // Nếu user có các sở thích tùy chỉnh đã lưu trước đó không nằm trong danh sách mặc định
        if (savedInterests != null) {
            for (String saved : savedInterests) {
                if (saved != null && !saved.trim().isEmpty() && !allCategories.contains(saved.trim())) {
                    allCategories.add(saved.trim());
                }
            }
        }

        for (String category : allCategories) {
            boolean isChecked = savedInterests != null && savedInterests.contains(category);
            Chip chip = createInterestChip(category, isChecked);
            int addIndex = chipGroupInterests.indexOfChild(chipAddInterest);
            if (addIndex >= 0) {
                chipGroupInterests.addView(chip, addIndex);
            } else {
                chipGroupInterests.addView(chip);
            }
        }

        updateSelectedInterestsCount();

        if (chipAddInterest != null) {
            chipAddInterest.setOnClickListener(v -> showAddCategoryDialog());
        }
    }

    private Chip createInterestChip(String categoryName, boolean isChecked) {
        Chip chip = new Chip(this);
        chip.setText(categoryName);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setCheckedIconVisible(false); // Tắt icon chấm tròn/checkmark để chiều rộng giữ nguyên tuyệt đối, không bị nhảy dòng
        chip.setChipIconVisible(false);
        chip.setEnsureMinTouchTargetSize(false);
        chip.setChipBackgroundColorResource(R.color.chip_interest_background);
        chip.setTextColor(ContextCompat.getColorStateList(this, R.color.chip_interest_text));
        chip.setChipStrokeColorResource(R.color.chip_interest_stroke);
        chip.setChipStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        chip.setChecked(isChecked);

        chip.setOnCheckedChangeListener((buttonView, isNowChecked) -> {
            updateSelectedInterestsCount();
        });
        return chip;
    }

    private void updateSelectedInterestsCount() {
        int count = getSelectedInterests().size();
        if (tvSelectedInterestsCount != null) {
            tvSelectedInterestsCount.setText(count + " đã chọn");
        }
    }

    private List<String> getSelectedInterests() {
        List<String> list = new ArrayList<>();
        if (chipGroupInterests != null) {
            for (int i = 0; i < chipGroupInterests.getChildCount(); i++) {
                View child = chipGroupInterests.getChildAt(i);
                if (child instanceof Chip && child.getId() != R.id.chipAddInterest) {
                    Chip chip = (Chip) child;
                    if (chip.isChecked()) {
                        list.add(chip.getText().toString());
                    }
                }
            }
        }
        return list;
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thêm thể loại sách yêu thích");

        final EditText input = new EditText(this);
        input.setHint("Ví dụ: Kinh doanh, Khoa học viễn tưởng...");
        input.setSingleLine(true);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        FrameLayout container = new FrameLayout(this);
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Thêm", (dialog, which) -> {
            String newCat = input.getText().toString().trim();
            if (!newCat.isEmpty()) {
                boolean exists = false;
                for (int i = 0; i < chipGroupInterests.getChildCount(); i++) {
                    View child = chipGroupInterests.getChildAt(i);
                    if (child instanceof Chip && child.getId() != R.id.chipAddInterest) {
                        Chip c = (Chip) child;
                        if (c.getText().toString().equalsIgnoreCase(newCat)) {
                            c.setChecked(true);
                            exists = true;
                            break;
                        }
                    }
                }
                if (!exists) {
                    Chip chip = createInterestChip(newCat, true);
                    int addIndex = chipGroupInterests.indexOfChild(chipAddInterest);
                    if (addIndex >= 0) {
                        chipGroupInterests.addView(chip, addIndex);
                    } else {
                        chipGroupInterests.addView(chip);
                    }
                }
                updateSelectedInterestsCount();
                Toast.makeText(this, "Đã thêm thể loại: " + newCat, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
