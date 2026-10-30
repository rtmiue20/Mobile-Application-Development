package vn.edu.ueh.rtmiue20.project.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "book_table")
public class Book {
    // TC-ADMIN-06: Trạng thái kiểm duyệt bài đăng
    public static final String STATUS_AVAILABLE = "available";        // Đã duyệt - Tặng miễn phí
    public static final String STATUS_EXCHANGING = "exchanging";      // Đã duyệt - Đang trao đổi
    public static final String STATUS_PENDING_REVIEW = "pending_review"; // Member mới đăng, chờ Admin duyệt
    public static final String STATUS_HIDDEN = "hidden";              // Admin ẩn bài vi phạm
    public static final String STATUS_GIVEN = "given";                // Đã tặng
    public static final String STATUS_EXCHANGED = "exchanged";        // Đã đổi

    /** Sách đang chờ Admin kiểm duyệt (dùng static để Firestore/Room không coi là field). */
    public static boolean isPendingReview(Book b) {
        return b != null && STATUS_PENDING_REVIEW.equalsIgnoreCase(b.getStatus());
    }

    /**
     * Trạng thái công khai sau khi Admin phê duyệt: suy ra từ hình thức chia sẻ
     * mà Member đã chọn (lưu trong mô tả bởi CreatePostActivity).
     */
    public static String resolveApprovedStatus(Book b) {
        String desc = (b != null && b.getDescription() != null) ? b.getDescription() : "";
        return desc.contains("Trao đổi sách khác") ? STATUS_EXCHANGING : STATUS_AVAILABLE;
    }

    @PrimaryKey
    @NonNull
    private String id = "";
    private String title;
    private String author;
    private String description;
    private String imageUrl;
    private String ownerId;
    private long createdAt;
    private String category;
    private String condition;
    private String status = "available";
    private int viewCount = 0;
    private int favoriteCount = 0;

    // Hình thức và Giá độc lập
    private String sharingType;
    private long price = 0L;

    // Tọa độ & địa chỉ thực tế của sách để tính khoảng cách chính xác đến người tìm sách
    private String city;
    private String district;
    private String ward;
    private Double latitude;
    private Double longitude;

    public Book() {}   // Firestore bắt buộc có constructor rỗng (slide 90)

    @androidx.room.Ignore
    public Book(@NonNull String id, String title, String author, String description, String imageUrl, String ownerId, long createdAt, String category, String condition, String status) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.description = description;
        this.imageUrl = imageUrl;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.category = category;
        this.condition = condition;
        this.status = (status != null && !status.isEmpty()) ? status : "available";
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getViewCount() { return Math.max(0, viewCount); }
    public void setViewCount(int viewCount) { this.viewCount = Math.max(0, viewCount); }

    public int getFavoriteCount() { return Math.max(0, favoriteCount); }
    public void setFavoriteCount(int favoriteCount) { this.favoriteCount = Math.max(0, favoriteCount); }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getSharingType() { return sharingType; }
    public void setSharingType(String sharingType) { this.sharingType = sharingType; }

    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }

    public String getLocationAddressFormatted() {
        StringBuilder sb = new StringBuilder();
        if (ward != null && !ward.trim().isEmpty()) sb.append(ward.trim());
        if (district != null && !district.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(district.trim());
        }
        if (city != null && !city.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city.trim());
        }
        return sb.toString();
    }
}
