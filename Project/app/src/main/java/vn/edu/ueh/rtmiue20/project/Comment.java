package vn.edu.ueh.rtmiue20.project;

import com.google.firebase.firestore.Exclude;

public class Comment {

    private String commentId;
    private String userId;
    private String userName;
    private String content;
    private Long createdAt;

    // Constructor rong bat buoc cho Firebase Firestore
    public Comment() {
    }

    public Comment(String commentId, String userId, String userName, String content, Long createdAt) {
        this.commentId = commentId;
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.createdAt = createdAt;
    }

    // commentId lay tu document id nen khong luu vao field cua document
    @Exclude
    public String getCommentId() {
        return commentId;
    }

    @Exclude
    public void setCommentId(String commentId) {
        this.commentId = commentId;
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
}