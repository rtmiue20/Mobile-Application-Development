package vn.edu.ueh.rtmiue20.project.util;

import android.location.Location;

public class LocationHelper {
    /**
     * Tọa độ cố định của điểm hẹn nhận trao đổi sách:
     * Đại học Kinh Tế TP. Hồ Chí Minh (UEH) cơ sở B
     * Địa chỉ: 279 Nguyễn Tri Phương, Phường Diên Hồng (Phường 5 cũ), Quận 10, TP. Hồ Chí Minh
     */
    public static final double UEH_CAMPUS_B_LAT = 10.7610;
    public static final double UEH_CAMPUS_B_LNG = 106.6663;
    public static final String UEH_CAMPUS_B_ADDRESS = "279 Nguyễn Tri Phương, Phường Diên Hồng, Quận 10, TP. Hồ Chí Minh";

    /**
     * Hàm tính khoảng cách giữa 2 tọa độ
     * @return Khoảng cách định dạng chuỗi (Ví dụ: "800m" hoặc "1.2 km")
     */
    public static String calculateDistance(double userLat, double userLng, double targetLat, double targetLng) {
        if ((userLat == 0.0 && userLng == 0.0) || (targetLat == 0.0 && targetLng == 0.0)) {
            return "800m";
        }
        float[] results = new float[1];
        Location.distanceBetween(userLat, userLng, targetLat, targetLng, results);
        float distanceInMeters = results[0];

        if (distanceInMeters < 1000) {
            return Math.round(distanceInMeters) + "m";
        } else {
            return String.format(java.util.Locale.getDefault(), "%.1f km", distanceInMeters / 1000f);
        }
    }

    /**
     * Đo khoảng cách từ vị trí của người dùng đến UEH cơ sở B (279 Nguyễn Tri Phương)
     */
    public static String getDistanceToUEHCampusB(double userLat, double userLng) {
        if (userLat == 0.0 && userLng == 0.0) {
            return "800m";
        }
        return calculateDistance(userLat, userLng, UEH_CAMPUS_B_LAT, UEH_CAMPUS_B_LNG);
    }
}