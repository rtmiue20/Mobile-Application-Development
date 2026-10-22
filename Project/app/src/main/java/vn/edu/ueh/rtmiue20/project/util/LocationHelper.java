package vn.edu.ueh.rtmiue20.project.util;

import android.location.Location;

public class LocationHelper {
    /**
     * Hàm tính khoảng cách giữa 2 tọa độ (Người dùng và Chủ sách)
     * @return Khoảng cách định dạng chuỗi (Ví dụ: "800m" hoặc "1.2km")
     */
    public static String calculateDistance(double userLat, double userLng, double ownerLat, double ownerLng) {
        float[] results = new float[1];
        Location.distanceBetween(userLat, userLng, ownerLat, ownerLng, results);
        float distanceInMeters = results[0];

        if (distanceInMeters < 1000) {
            return Math.round(distanceInMeters) + "m";
        } else {
            return String.format(java.util.Locale.getDefault(), "%.1f km", distanceInMeters / 1000f);
        }
    }
}