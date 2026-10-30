package vn.edu.ueh.rtmiue20.project.data.model;

import com.google.firebase.firestore.Exclude;

public class Comment {

    private String commentId;
    private String bookId; // Khóa ngoại REF tới document trong collection books
    private String userId;
    private String userName;
    private String userAvatar;
    private String content;
    private Long createdAt;
    private Long updatedAt; // Thêm để theo dõi thời gian chỉnh sửa (Update)

    public Comment() {
    }

    public Comment(String commentId, String bookId, String userId, String userName, String userAvatar, String content, Long createdAt) {
        this.commentId = commentId;
        this.bookId = bookId;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = null;
    }

    public Comment(String commentId, String userId, String userName, String userAvatar, String content, Long createdAt) {
        this(commentId, null, userId, userName, userAvatar, content, createdAt);
    }

    public Comment(String commentId, String userId, String userName, String content, Long createdAt) {
        this(commentId, null, userId, userName, "", content, createdAt);
    }

    @Exclude
    public String getCommentId() {
        return commentId;
    }

    @Exclude
    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
