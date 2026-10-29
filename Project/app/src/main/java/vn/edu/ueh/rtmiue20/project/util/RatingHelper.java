package vn.edu.ueh.rtmiue20.project.util;

import java.util.List;
import java.util.Locale;

/**
 * Lớp tiện ích tính toán và định dạng điểm đánh giá uy tín (Rating / Review) cho người dùng.
 */
public class RatingHelper {

    /**
     * Phương thức tính điểm trung bình mới khi người dùng nhận thêm một lượt đánh giá.
     * @param currentAvg Điểm trung bình hiện tại (ví dụ 5.0)
     * @param currentCount Số lượt đánh giá hiện tại (ví dụ 2)
     * @param newRating Điểm đánh giá mới (1.0 đến 5.0)
     * @return Điểm trung bình mới được làm tròn 1 chữ số thập phân (ví dụ 4.8)
     */
    public static double calculateNewAverage(double currentAvg, int currentCount, float newRating) {
        if (currentCount <= 0) {
            return Math.round(newRating * 10.0) / 10.0;
        }
        double totalScore = (currentAvg * currentCount) + newRating;
        int newCount = currentCount + 1;
        double avg = totalScore / newCount;
        return Math.round(avg * 10.0) / 10.0;
    }

    /**
     * Phương thức tính điểm trung bình từ danh sách tất cả các điểm đánh giá đã nhận.
     * @param ratings Danh sách các điểm đánh giá (1 - 5 sao)
     * @return Điểm trung bình làm tròn 1 chữ số thập phân
     */
    public static double calculateAverageRating(List<Float> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            return 5.0; // Điểm khởi đầu mặc định khi chưa có giao dịch
        }
        double sum = 0.0;
        int validCount = 0;
        for (Float r : ratings) {
            if (r != null && r > 0) {
                sum += r;
                validCount++;
            }
        }
        if (validCount == 0) return 5.0;
        double avg = sum / validCount;
        return Math.round(avg * 10.0) / 10.0;
    }

    /**
     * Định dạng chuỗi hiển thị điểm đánh giá trên hồ sơ người dùng (ví dụ: "5.0 (2)" hoặc "4.8 (5)")
     */
    public static String formatRating(double rating, int count) {
        if (count > 0) {
            return String.format(Locale.US, "%.1f (%d)", rating, count);
        } else {
            return "5.0 (0)";
        }
    }

    /**
     * Tạo danh hiệu uy tín dựa trên điểm số và số lượng giao dịch thành công.
     */
    public static String getRatingBadgeTitle(double rating, int count) {
        if (count <= 0) {
            return "✔ Thành viên BookShare UEH";
        }
        if (rating >= 4.8) {
            return String.format(Locale.US, "✔ Thành viên uy tín 5 sao (%.1f★ • %d lượt)", rating, count);
        } else if (rating >= 4.0) {
            return String.format(Locale.US, "✔ Thành viên tích cực (%.1f★ • %d lượt)", rating, count);
        } else {
            return String.format(Locale.US, "✔ Độc giả BookShare (%.1f★ • %d lượt)", rating, count);
        }
    }
}
