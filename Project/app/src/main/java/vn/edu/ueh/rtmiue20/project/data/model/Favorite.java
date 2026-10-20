package vn.edu.ueh.rtmiue20.project.data.model;

public class Favorite {

    private String favoriteId;
    private String userId;
    private String bookId;
    private Long timestamp;

    // Constructor rong bat buoc cho Firebase Firestore
    public Favorite() {
    }

    public Favorite(String favoriteId, String userId, String bookId, Long timestamp) {
        this.favoriteId = favoriteId;
        this.userId = userId;
        this.bookId = bookId;
        this.timestamp = timestamp;
    }

    public String getFavoriteId() {
        return favoriteId;
    }

    public void setFavoriteId(String favoriteId) {
        this.favoriteId = favoriteId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
