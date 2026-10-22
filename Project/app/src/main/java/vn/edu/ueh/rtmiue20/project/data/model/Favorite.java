package vn.edu.ueh.rtmiue20.project.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorite_table")
public class Favorite {

    @PrimaryKey
    @NonNull
    private String favoriteId = "";
    private String userId;
    private String bookId;
    private Long timestamp;

    public Favorite() {
    }

    public Favorite(@NonNull String favoriteId, String userId, String bookId, Long timestamp) {
        this.favoriteId = favoriteId;
        this.userId = userId;
        this.bookId = bookId;
        this.timestamp = timestamp;
    }

    @NonNull
    public String getFavoriteId() { return favoriteId; }
    public void setFavoriteId(@NonNull String favoriteId) { this.favoriteId = favoriteId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}