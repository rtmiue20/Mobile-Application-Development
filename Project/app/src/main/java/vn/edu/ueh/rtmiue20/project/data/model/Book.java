package vn.edu.ueh.rtmiue20.project.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "book_table")
public class Book {
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
    public void setViewCount(long viewCount) { this.viewCount = (int) Math.max(0, viewCount); }

    public int getFavoriteCount() { return Math.max(0, favoriteCount); }
    public void setFavoriteCount(int favoriteCount) { this.favoriteCount = Math.max(0, favoriteCount); }
    public void setFavoriteCount(long favoriteCount) { this.favoriteCount = (int) Math.max(0, favoriteCount); }
}
